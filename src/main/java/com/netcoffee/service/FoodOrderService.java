package com.netcoffee.service;

import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.enumtype.OrderStatusEnum;

import java.util.List;

public interface FoodOrderService
{

    OrderResponse createOrder(Long userId, CreateOrderRequest request);

    OrderResponse updateStatus(Long orderId, OrderStatusEnum status, Long confirmedByUserId);

    OrderResponse cancelOrder(Long orderId, String reason, Long cancelledByUserId);

    List<OrderResponse> findBySessionId(Long sessionId);

    List<OrderResponse> findPendingOrders();

    List<OrderResponse> findAll();
}
