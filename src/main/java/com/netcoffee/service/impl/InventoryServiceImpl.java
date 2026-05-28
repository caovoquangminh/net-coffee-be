package com.netcoffee.service.impl;

import com.netcoffee.dto.request.CreateInventoryItemRequest;
import com.netcoffee.dto.request.ExportStockRequest;
import com.netcoffee.dto.request.ImportStockRequest;
import com.netcoffee.dto.response.InventoryItemResponse;
import com.netcoffee.dto.response.InventoryTransactionResponse;
import com.netcoffee.entity.TInventoryItemEntity;
import com.netcoffee.entity.TInventoryTransactionEntity;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.*;
import com.netcoffee.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final SupplierRepository supplierRepository;
    private final MenuItemRepository menuItemRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public InventoryItemResponse createItem(CreateInventoryItemRequest request) {
        TInventoryItemEntity entity = TInventoryItemEntity.builder()
                .name(request.getName())
                .unit(request.getUnit())
                .minStock(request.getMinStock())
                .menuItemId(request.getMenuItemId())
                .description(request.getDescription())
                .build();
        return toItemResponse(inventoryItemRepository.save(entity));
    }

    @Override
    @Transactional
    public InventoryItemResponse updateItem(Long id, CreateInventoryItemRequest request) {
        TInventoryItemEntity entity = findItemEntity(id);
        entity.setName(request.getName());
        entity.setUnit(request.getUnit());
        entity.setMinStock(request.getMinStock());
        entity.setMenuItemId(request.getMenuItemId());
        entity.setDescription(request.getDescription());
        return toItemResponse(inventoryItemRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findAllItems() {
        return inventoryItemRepository.findAll().stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<InventoryItemResponse> findLowStockItems() {
        return inventoryItemRepository.findLowStock().stream()
                .map(this::toItemResponse)
                .toList();
    }

    @Override
    @Transactional
    public InventoryTransactionResponse importStock(Long performedByUserId, ImportStockRequest request) {
        TInventoryItemEntity item = findItemEntity(request.getInventoryItemId());

        boolean wasZero = item.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
        item.setCurrentStock(item.getCurrentStock().add(request.getQuantity()));
        inventoryItemRepository.save(item);

        // Re-enable linked menu item when restocked from zero
        if (wasZero && item.getMenuItemId() != null) {
            menuItemRepository.findById(item.getMenuItemId()).ifPresent(mi -> {
                if (!mi.getIsAvailable()) {
                    mi.setIsAvailable(true);
                    menuItemRepository.save(mi);
                }
            });
        }

        TInventoryTransactionEntity tx = TInventoryTransactionEntity.builder()
                .inventoryItemId(item.getId())
                .supplierId(request.getSupplierId())
                .type(InventoryTransactionTypeEnum.IMPORT)
                .quantity(request.getQuantity())
                .purchasePrice(request.getPurchasePrice())
                .expiryDate(request.getExpiryDate())
                .notes(request.getNotes())
                .performedBy(performedByUserId)
                .build();

        return toTxResponse(transactionRepository.save(tx));
    }

    @Override
    @Transactional
    public InventoryTransactionResponse exportStock(Long performedByUserId, ExportStockRequest request) {
        TInventoryItemEntity item = findItemEntity(request.getInventoryItemId());

        if (item.getCurrentStock().compareTo(request.getQuantity()) < 0) {
            throw new IllegalArgumentException("Tồn kho không đủ: còn " + item.getCurrentStock() + " " + item.getUnit());
        }

        item.setCurrentStock(item.getCurrentStock().subtract(request.getQuantity()));
        inventoryItemRepository.save(item);

        TInventoryTransactionEntity tx = TInventoryTransactionEntity.builder()
                .inventoryItemId(item.getId())
                .type(InventoryTransactionTypeEnum.EXPORT)
                .quantity(request.getQuantity())
                .notes(request.getNotes())
                .performedBy(performedByUserId)
                .build();

        return toTxResponse(transactionRepository.save(tx));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getTransactionHistory(Long inventoryItemId, Pageable pageable) {
        return transactionRepository
                .findByInventoryItemIdOrderByCreatedAtDesc(inventoryItemId, pageable)
                .map(this::toTxResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> getAllTransactions(Pageable pageable) {
        return transactionRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toTxResponse);
    }

    private TInventoryItemEntity findItemEntity(Long id) {
        return inventoryItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mặt hàng kho không tồn tại: " + id));
    }

    private InventoryItemResponse toItemResponse(TInventoryItemEntity e) {
        String menuItemName = null;
        if (e.getMenuItemId() != null) {
            menuItemName = menuItemRepository.findById(e.getMenuItemId())
                    .map(m -> m.getName())
                    .orElse(null);
        }
        boolean outOfStock = e.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0;
        boolean lowStock = !outOfStock
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
                .menuItemId(e.getMenuItemId())
                .menuItemName(menuItemName)
                .description(e.getDescription())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private InventoryTransactionResponse toTxResponse(TInventoryTransactionEntity tx) {
        String itemName = inventoryItemRepository.findById(tx.getInventoryItemId())
                .map(TInventoryItemEntity::getName).orElse(null);
        String itemUnit = inventoryItemRepository.findById(tx.getInventoryItemId())
                .map(TInventoryItemEntity::getUnit).orElse(null);
        String supplierName = null;
        if (tx.getSupplierId() != null) {
            supplierName = supplierRepository.findById(tx.getSupplierId())
                    .map(s -> s.getName()).orElse(null);
        }
        String performedByName = userRepository.findById(tx.getPerformedBy())
                .map(u -> u.getFullName() != null ? u.getFullName() : u.getPhoneNumber())
                .orElse(null);

        return InventoryTransactionResponse.builder()
                .id(tx.getId())
                .inventoryItemId(tx.getInventoryItemId())
                .inventoryItemName(itemName)
                .supplierId(tx.getSupplierId())
                .supplierName(supplierName)
                .type(tx.getType())
                .quantity(tx.getQuantity())
                .unit(itemUnit)
                .purchasePrice(tx.getPurchasePrice())
                .expiryDate(tx.getExpiryDate())
                .notes(tx.getNotes())
                .performedBy(tx.getPerformedBy())
                .performedByName(performedByName)
                .createdAt(tx.getCreatedAt())
                .build();
    }
}
