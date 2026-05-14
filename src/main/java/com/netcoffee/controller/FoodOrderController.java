package com.netcoffee.controller;

import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.service.FoodOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/orders") @RequiredArgsConstructor
public class FoodOrderController
{

    private final FoodOrderService foodOrderService;

    @PostMapping
    public ResponseEntity<ApiResponse<OrderResponse>> createOrder(@AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest request)
    {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Đặt món thành công", foodOrderService.createOrder(userId, request)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable Long id,
            @RequestParam OrderStatusEnum status)
    {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.updateStatus(id, status)));
    }

    @GetMapping("/session/{sessionId}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getBySession(@PathVariable Long sessionId)
    {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findBySessionId(sessionId)));
    }

    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPending()
    {
        return ResponseEntity.ok(ApiResponse.ok(foodOrderService.findPendingOrders()));
    }
}
