package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.CancelOrderRequest;
import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.service.FoodOrderService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ORDERS)
@RequiredArgsConstructor
public class FoodOrderController {

    private final FoodOrderService foodOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(
                        ApiResponse.ok(
                                "Đặt món thành công",
                                foodOrderService.createOrder(userId, request)));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam OrderStatusEnum status) {
        Long staffId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(foodOrderService.updateStatus(id, status, staffId)));
    }

    @PostMapping("/{id}/confirm-payment")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmPayment(
            @AuthenticationPrincipal UserDetails userDetails, @PathVariable Long id) {
        Long staffId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Xác nhận thanh toán thành công",
                        foodOrderService.confirmPayment(id, staffId)));
    }

    @PatchMapping("/{id}/cancel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody(required = false) CancelOrderRequest body) {
        Long adminId = Long.parseLong(userDetails.getUsername());
        String reason = body != null && body.getReason() != null ? body.getReason() : "";
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.cancelOrder(id, reason, adminId)));
    }

    @GetMapping("/session/{sessionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getBySession(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findBySessionId(sessionId)));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findPendingOrders()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findAll()));
    }
}
