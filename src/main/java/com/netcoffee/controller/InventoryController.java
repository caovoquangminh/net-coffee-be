package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.CreateInventoryItemRequest;
import com.netcoffee.dto.request.ExportStockRequest;
import com.netcoffee.dto.request.ImportStockRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.InventoryItemResponse;
import com.netcoffee.dto.response.InventoryTransactionResponse;
import com.netcoffee.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.INVENTORY)
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    // ── Items ────────────────────────────────────────────────────────────────

    @GetMapping("/items")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getAllItems() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.findAllItems()));
    }

    @GetMapping("/items/low-stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getLowStock() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.findLowStockItems()));
    }

    @PostMapping("/items")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> createItem(
            @Valid @RequestBody CreateInventoryItemRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Thêm mặt hàng thành công", inventoryService.createItem(request)));
    }

    @PutMapping("/items/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> updateItem(
            @PathVariable Long id, @Valid @RequestBody CreateInventoryItemRequest request) {
        return ResponseEntity.ok(
                ApiResponse.ok("Cập nhật thành công", inventoryService.updateItem(id, request)));
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    @PostMapping("/import")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> importStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ImportStockRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Nhập kho thành công",
                                inventoryService.importStock(userId, request)));
    }

    @PostMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<InventoryTransactionResponse>> exportStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ExportStockRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Xuất kho thành công",
                                inventoryService.exportStock(userId, request)));
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionResponse>>> getAllTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getAllTransactions(pageable)));
    }

    @GetMapping("/transactions/item/{itemId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Page<InventoryTransactionResponse>>> getByItem(
            @PathVariable Long itemId, @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(
                ApiResponse.ok(inventoryService.getTransactionHistory(itemId, pageable)));
    }
}
