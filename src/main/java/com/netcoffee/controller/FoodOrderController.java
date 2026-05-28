package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.service.FoodOrderService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestParam OrderStatusEnum status) {
        Long staffId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(foodOrderService.updateStatus(id, status, staffId)));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Long staffId = Long.parseLong(userDetails.getUsername());
        String reason = body.getOrDefault("reason", "");
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.cancelOrder(id, reason, staffId)));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getBySession(
            @PathVariable Long sessionId) {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findBySessionId(sessionId)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPending() {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findPendingOrders()));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findAll()));
    }
}
