package com.netcoffee.service.impl;

import com.netcoffee.dto.response.MenuItemResponse;
import com.netcoffee.entity.TInventoryItemEntity;
import com.netcoffee.entity.TMenuItemAddonEntity;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.entity.TMenuItemInventoryEntity;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.InventoryItemRepository;
import com.netcoffee.repository.MenuItemAddonRepository;
import com.netcoffee.repository.MenuItemInventoryRepository;
import com.netcoffee.repository.MenuItemRepository;
import com.netcoffee.service.MenuItemService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemAddonRepository addonRepository;
    private final MenuItemInventoryRepository menuItemInventoryRepository;
    private final InventoryItemRepository inventoryItemRepository;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAvailable() {
        return menuItemRepository.findByIsAvailableTrue().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAllForClient() {
        List<TMenuItemEntity> items = menuItemRepository.findAll();

        Map<Long, TInventoryItemEntity> invById =
                inventoryItemRepository.findAll().stream()
                        .collect(Collectors.toMap(TInventoryItemEntity::getId, i -> i));

        Map<Long, List<TMenuItemInventoryEntity>> linksByMenuItem =
                menuItemInventoryRepository.findAll().stream()
                        .collect(Collectors.groupingBy(TMenuItemInventoryEntity::getMenuItemId));

        Map<Long, List<TMenuItemAddonEntity>> addonsByMenuItem =
                addonRepository.findAll().stream()
                        .collect(Collectors.groupingBy(TMenuItemAddonEntity::getMenuItemId));

        return items.stream()
                .map(
                        item -> {
                            List<TMenuItemInventoryEntity> links =
                                    linksByMenuItem.getOrDefault(item.getId(), List.of());

                            if (links.isEmpty()) {
                                return MenuItemResponse.builder()
                                        .id(item.getId())
                                        .name(item.getName())
                                        .price(item.getPrice())
                                        .category(item.getCategory())
                                        .imageUrl(item.getImageUrl())
                                        .description(item.getDescription())
                                        .isAvailable(false)
                                        .addons(List.of())
                                        .linkedInventoryItems(List.of())
                                        .build();
                            }

                            boolean stockBlocked =
                                    links.stream()
                                            .anyMatch(
                                                    link -> {
                                                        TInventoryItemEntity inv =
                                                                invById.get(
                                                                        link.getInventoryItemId());
                                                        if (inv == null) return true;
                                                        return inv.getCurrentStock()
                                                                        .compareTo(
                                                                                link.getQuantity())
                                                                < 0;
                                                    });

                            boolean effectivelyAvailable = item.getIsAvailable() && !stockBlocked;

                            List<MenuItemResponse.AddonResponse> addons =
                                    addonsByMenuItem.getOrDefault(item.getId(), List.of()).stream()
                                            .map(
                                                    a -> {
                                                        TInventoryItemEntity addonInv =
                                                                a.getInventoryItemId() != null
                                                                        ? invById.get(
                                                                                a
                                                                                        .getInventoryItemId())
                                                                        : null;
                                                        boolean addonAvailable =
                                                                a.getIsAvailable()
                                                                        && (addonInv == null
                                                                                || addonInv.getCurrentStock()
                                                                                                .compareTo(
                                                                                                        BigDecimal
                                                                                                                .ONE)
                                                                                        >= 0);
                                                        return MenuItemResponse.AddonResponse
                                                                .builder()
                                                                .id(a.getId())
                                                                .name(a.getName())
                                                                .extraPrice(a.getExtraPrice())
                                                                .isAvailable(addonAvailable)
                                                                .inventoryItemId(
                                                                        a.getInventoryItemId())
                                                                .build();
                                                    })
                                            .toList();

                            List<MenuItemResponse.InventoryLinkResponse> linkedInventoryItems =
                                    links.stream()
                                            .map(
                                                    link -> {
                                                        TInventoryItemEntity inv =
                                                                invById.get(
                                                                        link.getInventoryItemId());
                                                        return MenuItemResponse
                                                                .InventoryLinkResponse.builder()
                                                                .inventoryItemId(
                                                                        link.getInventoryItemId())
                                                                .inventoryItemName(
                                                                        inv != null
                                                                                ? inv.getName()
                                                                                : "?")
                                                                .quantity(link.getQuantity())
                                                                .build();
                                                    })
                                            .toList();

                            return MenuItemResponse.builder()
                                    .id(item.getId())
                                    .name(item.getName())
                                    .price(item.getPrice())
                                    .category(item.getCategory())
                                    .imageUrl(item.getImageUrl())
                                    .description(item.getDescription())
                                    .isAvailable(effectivelyAvailable)
                                    .addons(addons)
                                    .linkedInventoryItems(linkedInventoryItems)
                                    .build();
                        })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findByCategory(String category) {
        return menuItemRepository.findByCategoryAndIsAvailableTrue(category).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TMenuItemEntity findById(Long id) {
        return menuItemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại: " + id));
    }

    @Override
    public MenuItemResponse toResponse(TMenuItemEntity item) {
        return buildResponse(item, false);
    }

    @Override
    public MenuItemResponse toAdminResponse(TMenuItemEntity item) {
        return buildResponse(item, true);
    }

    private MenuItemResponse buildResponse(TMenuItemEntity item, boolean includeUnavailableAddons) {
        List<MenuItemResponse.AddonResponse> addons =
                (includeUnavailableAddons
                                ? addonRepository.findByMenuItemId(item.getId())
                                : addonRepository.findByMenuItemIdAndIsAvailable(
                                        item.getId(), true))
                        .stream()
                                .map(
                                        a ->
                                                MenuItemResponse.AddonResponse.builder()
                                                        .id(a.getId())
                                                        .name(a.getName())
                                                        .extraPrice(a.getExtraPrice())
                                                        .isAvailable(a.getIsAvailable())
                                                        .inventoryItemId(a.getInventoryItemId())
                                                        .build())
                                .toList();

        List<TMenuItemInventoryEntity> links =
                menuItemInventoryRepository.findByMenuItemId(item.getId());

        Map<Long, TInventoryItemEntity> invById =
                inventoryItemRepository.findAll().stream()
                        .collect(Collectors.toMap(TInventoryItemEntity::getId, i -> i));

        List<MenuItemResponse.InventoryLinkResponse> linkedInventoryItems =
                links.stream()
                        .map(
                                link -> {
                                    TInventoryItemEntity inv =
                                            invById.get(link.getInventoryItemId());
                                    return MenuItemResponse.InventoryLinkResponse.builder()
                                            .inventoryItemId(link.getInventoryItemId())
                                            .inventoryItemName(inv != null ? inv.getName() : "?")
                                            .quantity(link.getQuantity())
                                            .build();
                                })
                        .toList();

        return MenuItemResponse.builder()
                .id(item.getId())
                .name(item.getName())
                .price(item.getPrice())
                .category(item.getCategory())
                .imageUrl(item.getImageUrl())
                .description(item.getDescription())
                .isAvailable(item.getIsAvailable())
                .addons(addons)
                .linkedInventoryItems(linkedInventoryItems)
                .build();
    }
}
