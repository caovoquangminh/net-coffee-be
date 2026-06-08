package com.netcoffee.service.impl;

import com.netcoffee.dto.request.CreateInventoryItemRequest;
import com.netcoffee.dto.request.ExportStockRequest;
import com.netcoffee.dto.request.ImportStockRequest;
import com.netcoffee.dto.response.InventoryItemResponse;
import com.netcoffee.dto.response.InventoryTransactionResponse;
import com.netcoffee.entity.TInventoryItemEntity;
import com.netcoffee.entity.TInventoryTransactionEntity;
import com.netcoffee.entity.TMenuItemInventoryEntity;
import com.netcoffee.entity.TSupplierEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.*;
import com.netcoffee.service.InventoryService;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final MenuItemInventoryRepository menuItemInventoryRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final SupplierRepository supplierRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemAddonRepository menuItemAddonRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {
        TInventoryItemEntity entity =
                TInventoryItemEntity.builder()
                        .name(request.getName())
                        .unit(request.getUnit())
                        .minStock(request.getMinStock())
                        .description(request.getDescription())
                        .build();
        entity = inventoryItemRepository.save(entity);
        saveMenuItemLinks(entity.getId(), request.getMenuItemIds());
        return toItemResponse(entity);
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, CreateInventoryItemRequest request) {
        TInventoryItemEntity entity = findItemEntity(id);
        entity.setName(request.getName());
        entity.setUnit(request.getUnit());
        entity.setMinStock(request.getMinStock());
        entity.setDescription(request.getDescription());
        entity = inventoryItemRepository.save(entity);
        menuItemInventoryRepository.deleteByInventoryItemId(entity.getId());
        saveMenuItemLinks(entity.getId(), request.getMenuItemIds());
        return toItemResponse(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findAllItems() {
        return inventoryItemRepository.findAll().stream().map(this::toItemResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findLowStockItems() {
        return inventoryItemRepository.findLowStock().stream().map(this::toItemResponse).toList();
    }

    @Override
    @Transactional
    public InventoryTransactionResponse importStock(
            Long performedByUserId, ImportStockRequest request) {
        TInventoryItemEntity item = findItemEntity(request.getInventoryItemId());

        boolean wasZero = item.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
        item.setCurrentStock(item.getCurrentStock().add(request.getQuantity()));
        inventoryItemRepository.save(item);

        if (wasZero) {
            List<Long> enabledMenuItemIds = new java.util.ArrayList<>();
            menuItemInventoryRepository
                    .findByInventoryItemId(item.getId())
                    .forEach(
                            link ->
                                    menuItemRepository
                                            .findById(link.getMenuItemId())
                                            .ifPresent(
                                                    mi -> {
                                                        if (Boolean.TRUE.equals(
                                                                mi.getDisabledByStock())) {
                                                            mi.setIsAvailable(true);
                                                            mi.setDisabledByStock(false);
                                                            menuItemRepository.save(mi);
                                                            enabledMenuItemIds.add(mi.getId());
                                                        }
                                                    }));
            menuItemAddonRepository
                    .findByInventoryItemId(item.getId())
                    .forEach(
                            addon -> {
                                if (Boolean.TRUE.equals(addon.getDisabledByStock())) {
                                    addon.setIsAvailable(true);
                                    addon.setDisabledByStock(false);
                                    menuItemAddonRepository.save(addon);
                                }
                            });
            if (!enabledMenuItemIds.isEmpty()) {
                messagingTemplate.convertAndSend(
                        "/topic/menu-updated",
                        Map.of("action", "ENABLED", "menuItemIds", enabledMenuItemIds));
            }
        }

        TInventoryTransactionEntity tx =
                TInventoryTransactionEntity.builder()
                        .inventoryItemId(item.getId())
                        .supplierId(request.getSupplierId())
                        .type(InventoryTransactionTypeEnum.IMPORT)
                        .quantity(request.getQuantity())
                        .purchasePrice(request.getPurchasePrice())
                        .expiryDate(request.getExpiryDate())
                        .notes(request.getNotes())
                        .performedBy(performedByUserId)
                        .build();

        TInventoryTransactionEntity savedTx = transactionRepository.save(tx);
        InventoryTransactionResponse txResponse =
                toTxResponse(savedTx, buildTxLookupMaps(List.of(savedTx)));

        messagingTemplate.convertAndSend(
                "/topic/admin/inventory/imported",
                Map.of(
                        "inventoryItemId", item.getId(),
                        "inventoryItemName", item.getName(),
                        "quantity", request.getQuantity(),
                        "unit", item.getUnit(),
                        "currentStock", item.getCurrentStock()));

        return txResponse;
    }

    @Override
    @Transactional
    public InventoryTransactionResponse exportStock(
            Long performedByUserId, ExportStockRequest request) {
        TInventoryItemEntity item = findItemEntity(request.getInventoryItemId());

        if (item.getCurrentStock().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException(
                    "Tồn kho không đủ: còn " + item.getCurrentStock() + " " + item.getUnit());
        }

        BigDecimal newStock = item.getCurrentStock().subtract(request.getQuantity());
        item.setCurrentStock(newStock);
        inventoryItemRepository.save(item);

        TInventoryTransactionEntity tx =
                TInventoryTransactionEntity.builder()
                        .inventoryItemId(item.getId())
                        .type(InventoryTransactionTypeEnum.EXPORT)
                        .quantity(request.getQuantity())
                        .notes(request.getNotes())
                        .performedBy(performedByUserId)
                        .build();
        transactionRepository.save(tx);

        if (newStock.compareTo(BigDecimal.ZERO) <= 0) {
            List<Long> disabledMenuItemIds = new java.util.ArrayList<>();
            menuItemInventoryRepository
                    .findByInventoryItemId(item.getId())
                    .forEach(
                            link ->
                                    menuItemRepository
                                            .findById(link.getMenuItemId())
                                            .ifPresent(
                                                    mi -> {
                                                        mi.setIsAvailable(false);
                                                        mi.setDisabledByStock(true);
                                                        menuItemRepository.save(mi);
                                                        disabledMenuItemIds.add(mi.getId());
                                                    }));
            menuItemAddonRepository
                    .findByInventoryItemId(item.getId())
                    .forEach(
                            addon -> {
                                addon.setIsAvailable(false);
                                addon.setDisabledByStock(true);
                                menuItemAddonRepository.save(addon);
                            });
            messagingTemplate.convertAndSend(
                    "/topic/admin/inventory/low-stock",
                    Map.of(
                            "inventoryItemId",
                            item.getId(),
                            "inventoryItemName",
                            item.getName(),
                            "currentStock",
                            0,
                            "unit",
                            item.getUnit(),
                            "minStock",
                            item.getMinStock(),
                            "outOfStock",
                            true));
            if (!disabledMenuItemIds.isEmpty()) {
                messagingTemplate.convertAndSend(
                        "/topic/menu-updated",
                        Map.of("action", "DISABLED", "menuItemIds", disabledMenuItemIds));
            }
        }

        return toTxResponse(tx, buildTxLookupMaps(List.of(tx)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getTransactionHistory(
            Long inventoryItemId, Pageable pageable) {
        Page<TInventoryTransactionEntity> page =
                transactionRepository.findByInventoryItemIdOrderByCreatedAtDesc(
                        inventoryItemId, pageable);
        return page.map(tx -> toTxResponse(tx, buildTxLookupMaps(page.getContent())));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getAllTransactions(Pageable pageable) {
        Page<TInventoryTransactionEntity> page =
                transactionRepository.findAllByOrderByCreatedAtDesc(pageable);
        return page.map(tx -> toTxResponse(tx, buildTxLookupMaps(page.getContent())));
    }

    private void saveMenuItemLinks(Long inventoryItemId, List<Long> menuItemIds) {
        if (menuItemIds == null || menuItemIds.isEmpty()) return;
        for (Long menuItemId : menuItemIds) {
            menuItemInventoryRepository.save(
                    new TMenuItemInventoryEntity(menuItemId, inventoryItemId));
        }
    }

    private TInventoryItemEntity findItemEntity(Long id) {
        return inventoryItemRepository
                .findById(id)
                .orElseThrow(
                        () -> new ResourceNotFoundException("Mặt hàng kho không tồn tại: " + id));
    }

    private InventoryItemResponse toItemResponse(TInventoryItemEntity e) {
        List<TMenuItemInventoryEntity> links =
                menuItemInventoryRepository.findByInventoryItemId(e.getId());
        List<Long> menuItemIds =
                links.stream().map(TMenuItemInventoryEntity::getMenuItemId).toList();
        List<String> menuItemNames =
                links.stream()
                        .map(
                                l ->
                                        menuItemRepository
                                                .findById(l.getMenuItemId())
                                                .map(m -> m.getName())
                                                .orElse(null))
                        .filter(n -> n != null)
                        .toList();

        boolean outOfStock = e.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock =
                !outOfStock
                        && e.getMinStock().compareTo(BigDecimal.ZERO) > 0
                        && e.getCurrentStock().compareTo(e.getMinStock()) <= 0;
        return InventoryItemResponse.builder()
                .id(e.getId())
                .name(e.getName())
                .unit(e.getUnit())
                .currentStock(e.getCurrentStock())
                .minStock(e.getMinStock())
                .outOfStock(outOfStock)
                .lowStock(lowStock)
                .menuItemIds(menuItemIds.isEmpty() ? Collections.emptyList() : menuItemIds)
                .menuItemNames(menuItemNames.isEmpty() ? Collections.emptyList() : menuItemNames)
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private record TxLookupMaps(
            Map<Long, TInventoryItemEntity> items,
            Map<Long, TSupplierEntity> suppliers,
            Map<Long, TUserEntity> users) {}

    private TxLookupMaps buildTxLookupMaps(List<TInventoryTransactionEntity> txList) {
        Set<Long> itemIds =
                txList.stream()
                        .map(TInventoryTransactionEntity::getInventoryItemId)
                        .collect(Collectors.toSet());
        Set<Long> supplierIds =
                txList.stream()
                        .map(TInventoryTransactionEntity::getSupplierId)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());
        Set<Long> userIds =
                txList.stream()
                        .map(TInventoryTransactionEntity::getPerformedBy)
                        .filter(id -> id != null)
                        .collect(Collectors.toSet());

        Map<Long, TInventoryItemEntity> items =
                inventoryItemRepository.findAllById(itemIds).stream()
                        .collect(Collectors.toMap(TInventoryItemEntity::getId, i -> i));
        Map<Long, TSupplierEntity> suppliers =
                supplierIds.isEmpty()
                        ? Collections.emptyMap()
                        : supplierRepository.findAllById(supplierIds).stream()
                                .collect(Collectors.toMap(TSupplierEntity::getId, s -> s));
        Map<Long, TUserEntity> users =
                userIds.isEmpty()
                        ? Collections.emptyMap()
                        : userRepository.findAllById(userIds).stream()
                                .collect(Collectors.toMap(TUserEntity::getId, u -> u));

        return new TxLookupMaps(items, suppliers, users);
    }

    private InventoryTransactionResponse toTxResponse(
            TInventoryTransactionEntity tx, TxLookupMaps maps) {
        TInventoryItemEntity item = maps.items().get(tx.getInventoryItemId());
        TSupplierEntity supplier =
                tx.getSupplierId() != null ? maps.suppliers().get(tx.getSupplierId()) : null;
        TUserEntity performer =
                tx.getPerformedBy() != null ? maps.users().get(tx.getPerformedBy()) : null;

        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .inventoryItemId(tx.getInventoryItemId())
                .inventoryItemName(item != null ? item.getName() : null)
                .supplierId(tx.getSupplierId())
                .supplierName(supplier != null ? supplier.getName() : null)
                .type(tx.getType())
                .quantity(tx.getQuantity())
                .unit(item != null ? item.getUnit() : null)
                .purchasePrice(tx.getPurchasePrice())
                .expiryDate(tx.getExpiryDate())
                .notes(tx.getNotes())
                .performedBy(tx.getPerformedBy())
                .performedByName(
                        performer != null
                                ? (performer.getFullName() != null
                                        ? performer.getFullName()
                                        : performer.getPhoneNumber())
                                : null)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
