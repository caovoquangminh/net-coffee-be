package com.netcoffee.service.impl;

import com.netcoffee.dto.request.CreateAddonRequest;
import com.netcoffee.dto.request.CreateMenuItemRequest;
import com.netcoffee.dto.request.SetInventoryLinksRequest;
import com.netcoffee.dto.response.MenuItemResponse;
import com.netcoffee.entity.TMenuItemAddonEntity;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.entity.TMenuItemInventoryEntity;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.MenuItemAddonRepository;
import com.netcoffee.repository.MenuItemInventoryRepository;
import com.netcoffee.repository.MenuItemRepository;
import com.netcoffee.service.MenuAdminService;
import com.netcoffee.service.MenuItemService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MenuAdminServiceImpl implements MenuAdminService {

    private final MenuItemRepository menuItemRepository;
    private final MenuItemAddonRepository addonRepository;
    private final MenuItemInventoryRepository menuItemInventoryRepository;
    private final MenuItemService menuItemService;

    @Override
    @Transactional(readOnly = true)
    public List<MenuItemResponse> findAll() {
        return menuItemRepository.findAll().stream().map(menuItemService::toAdminResponse).toList();
    }

    @Override
    @Transactional
    public MenuItemResponse create(CreateMenuItemRequest request) {
        TMenuItemEntity item =
                TMenuItemEntity.builder()
                        .name(request.getName())
                        .price(request.getPrice())
                        .category(request.getCategory())
                        .imageUrl(request.getImageUrl())
                        .description(request.getDescription())
                        .isAvailable(
                                request.getIsAvailable() != null ? request.getIsAvailable() : true)
                        .build();
        return menuItemService.toAdminResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public MenuItemResponse update(Long id, CreateMenuItemRequest request) {
        TMenuItemEntity item = findOrThrow(id);
        if (request.getName() != null) item.setName(request.getName());
        if (request.getPrice() != null) item.setPrice(request.getPrice());
        if (request.getCategory() != null) item.setCategory(request.getCategory());
        if (request.getImageUrl() != null) item.setImageUrl(request.getImageUrl());
        if (request.getDescription() != null) item.setDescription(request.getDescription());
        if (request.getIsAvailable() != null) item.setIsAvailable(request.getIsAvailable());
        return menuItemService.toAdminResponse(menuItemRepository.save(item));
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        TMenuItemEntity item = findOrThrow(id);
        item.setIsAvailable(false);
        menuItemRepository.save(item);
    }

    @Override
    @Transactional
    public MenuItemResponse addAddon(Long menuItemId, CreateAddonRequest request) {
        findOrThrow(menuItemId);
        TMenuItemAddonEntity addon =
                TMenuItemAddonEntity.builder()
                        .menuItemId(menuItemId)
                        .name(request.getName())
                        .extraPrice(request.getExtraPrice())
                        .inventoryItemId(request.getInventoryItemId())
                        .isAvailable(true)
                        .build();
        addonRepository.save(addon);
        return menuItemService.toAdminResponse(findOrThrow(menuItemId));
    }

    @Override
    @Transactional
    public MenuItemResponse updateAddon(Long menuItemId, Long addonId, CreateAddonRequest request) {
        findOrThrow(menuItemId);
        TMenuItemAddonEntity addon =
                addonRepository
                        .findById(addonId)
                        .filter(a -> a.getMenuItemId().equals(menuItemId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Addon không tồn tại: " + addonId));
        if (request.getName() != null) addon.setName(request.getName());
        if (request.getExtraPrice() != null) addon.setExtraPrice(request.getExtraPrice());
        if (request.getInventoryItemId() != null)
            addon.setInventoryItemId(request.getInventoryItemId());
        if (request.getIsAvailable() != null) addon.setIsAvailable(request.getIsAvailable());
        addonRepository.save(addon);
        return menuItemService.toAdminResponse(findOrThrow(menuItemId));
    }

    @Override
    @Transactional
    public void deleteAddon(Long menuItemId, Long addonId) {
        findOrThrow(menuItemId);
        TMenuItemAddonEntity addon =
                addonRepository
                        .findById(addonId)
                        .filter(a -> a.getMenuItemId().equals(menuItemId))
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Addon không tồn tại: " + addonId));
        addonRepository.delete(addon);
    }

    @Override
    @Transactional
    public MenuItemResponse setInventoryLinks(
            Long menuItemId, List<SetInventoryLinksRequest> links) {
        findOrThrow(menuItemId);
        menuItemInventoryRepository.deleteByMenuItemId(menuItemId);
        for (SetInventoryLinksRequest req : links) {
            BigDecimal qty =
                    (req.getQuantity() != null && req.getQuantity().compareTo(BigDecimal.ZERO) > 0)
                            ? req.getQuantity()
                            : BigDecimal.ONE;
            TMenuItemInventoryEntity entity = new TMenuItemInventoryEntity();
            entity.setMenuItemId(menuItemId);
            entity.setInventoryItemId(req.getInventoryItemId());
            entity.setQuantity(qty);
            menuItemInventoryRepository.save(entity);
        }
        return menuItemService.toAdminResponse(findOrThrow(menuItemId));
    }

    private TMenuItemEntity findOrThrow(Long id) {
        return menuItemRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Món không tồn tại: " + id));
    }
}
