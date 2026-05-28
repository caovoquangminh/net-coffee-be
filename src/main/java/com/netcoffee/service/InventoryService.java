package com.netcoffee.service;

import com.netcoffee.dto.request.CreateInventoryItemRequest;
import com.netcoffee.dto.request.ExportStockRequest;
import com.netcoffee.dto.request.ImportStockRequest;
import com.netcoffee.dto.response.InventoryItemResponse;
import com.netcoffee.dto.response.InventoryTransactionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface InventoryService {
    InventoryItemResponse createItem(CreateInventoryItemRequest request);
    InventoryItemResponse updateItem(Long id, CreateInventoryItemRequest request);
    List<InventoryItemResponse> findAllItems();
    List<InventoryItemResponse> findLowStockItems();

    InventoryTransactionResponse importStock(Long performedByUserId, ImportStockRequest request);
    InventoryTransactionResponse exportStock(Long performedByUserId, ExportStockRequest request);
    Page<InventoryTransactionResponse> getTransactionHistory(Long inventoryItemId, Pageable pageable);
    Page<InventoryTransactionResponse> getAllTransactions(Pageable pageable);
}
