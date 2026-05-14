package com.netcoffee.service.impl;

import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.entity.TFoodOrderEntity;
import com.netcoffee.entity.TFoodOrderItemEntity;
import com.netcoffee.entity.TMenuItemEntity;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.FoodOrderItemRepository;
import com.netcoffee.repository.FoodOrderRepository;
import com.netcoffee.service.FoodOrderService;
import com.netcoffee.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final MenuItemService menuItemService;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        // Build items + tính total
        List<TFoodOrderItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            TMenuItemEntity menuItem = menuItemService.findById(itemReq.getMenuItemId());
            if (!menuItem.getIsAvailable()) {
                throw new IllegalArgumentException("Món '" + menuItem.getName() + "' hiện không có sẵn");
            }
            BigDecimal subtotal = menuItem.getPrice()
                    .multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(subtotal);

            items.add(TFoodOrderItemEntity.builder()
                    .itemId(menuItem.getId())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.getPrice())  // snapshot giá
                    .build());
        }

        TFoodOrderEntity order = TFoodOrderEntity.builder()
                .sessionId(request.getSessionId())
                .userId(userId)
                .machineId(request.getMachineId())
                .status(OrderStatusEnum.PENDING)
                .totalPrice(total)
                .note(request.getNote())
                .build();

        order = foodOrderRepository.save(order);

        // Gán orderId cho items rồi save batch
        final Long orderId = order.getId();
        items.forEach(item -> item.setOrderId(orderId));
        foodOrderItemRepository.saveAll(items);

        return toResponse(order, items);
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(Long orderId, OrderStatusEnum status) {
        TFoodOrderEntity order = foodOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Đơn hàng không tồn tại: " + orderId));
        order.setStatus(status);
        order = foodOrderRepository.save(order);
        List<TFoodOrderItemEntity> items = foodOrderItemRepository.findByOrderId(orderId);
        return toResponse(order, items);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findBySessionId(Long sessionId) {
        return foodOrderRepository.findBySessionIdOrderByCreatedAtDesc(sessionId)
                .stream()
                .map(o -> toResponse(o, foodOrderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findPendingOrders() {
        return foodOrderRepository.findByStatus(OrderStatusEnum.PENDING)
                .stream()
                .map(o -> toResponse(o, foodOrderItemRepository.findByOrderId(o.getId())))
                .toList();
    }

    private OrderResponse toResponse(TFoodOrderEntity order, List<TFoodOrderItemEntity> items) {
        List<OrderResponse.OrderItemResponse> itemResponses = items.stream()
                .map(i -> OrderResponse.OrderItemResponse.builder()
                        .menuItemId(i.getItemId())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .subtotal(i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                        .build())
                .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .sessionId(order.getSessionId())
                .machineId(order.getMachineId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .note(order.getNote())
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
