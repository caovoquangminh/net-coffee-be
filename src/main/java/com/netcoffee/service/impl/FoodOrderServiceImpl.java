package com.netcoffee.service.impl;

import com.netcoffee.config.VietQrProperties;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.entity.*;
import com.netcoffee.enumtype.FoodOrderPaymentEnum;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.exception.ResourceNotFoundException;
import com.netcoffee.repository.*;
import com.netcoffee.repository.MenuItemRepository;
import com.netcoffee.service.FoodOrderService;
import com.netcoffee.service.MenuItemService;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodOrderItemAddonRepository foodOrderItemAddonRepository;
    private final MenuItemAddonRepository menuItemAddonRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final MenuItemService menuItemService;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final VietQrProperties vietQr;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        List<TFoodOrderItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            TMenuItemEntity menuItem = menuItemService.findById(itemReq.getMenuItemId());
            if (!menuItem.getIsAvailable()) {
                throw new IllegalArgumentException(
                        "Món '" + menuItem.getName() + "' hiện không có sẵn");
            }

            // Kiểm tra tồn kho
            List<TInventoryItemEntity> invItems =
                    inventoryItemRepository.findByMenuItemId(menuItem.getId());
            for (TInventoryItemEntity inv : invItems) {
                if (inv.getCurrentStock().compareTo(BigDecimal.valueOf(itemReq.getQuantity()))
                        < 0) {
                    throw new IllegalArgumentException(
                            "Món '" + menuItem.getName() + "' hiện đã hết hàng trong kho");
                }
            }

            BigDecimal addonTotal = BigDecimal.ZERO;
            if (itemReq.getAddonIds() != null && !itemReq.getAddonIds().isEmpty()) {
                List<TMenuItemAddonEntity> selectedAddons =
                        menuItemAddonRepository.findAllById(itemReq.getAddonIds());
                for (TMenuItemAddonEntity addon : selectedAddons) {
                    addonTotal = addonTotal.add(addon.getExtraPrice());
                }
            }

            BigDecimal unitTotal = menuItem.getPrice().add(addonTotal);
            total = total.add(unitTotal.multiply(BigDecimal.valueOf(itemReq.getQuantity())));

            items.add(
                    TFoodOrderItemEntity.builder()
                            .itemId(menuItem.getId())
                            .quantity(itemReq.getQuantity())
                            .unitPrice(menuItem.getPrice())
                            .build());
        }

        FoodOrderPaymentEnum paymentMethod =
                request.getPaymentMethod() != null
                        ? request.getPaymentMethod()
                        : FoodOrderPaymentEnum.CASH;

        TFoodOrderEntity order =
                TFoodOrderEntity.builder()
                        .sessionId(request.getSessionId())
                        .userId(userId)
                        .machineId(request.getMachineId())
                        .status(OrderStatusEnum.PENDING)
                        .totalPrice(total)
                        .note(request.getNote())
                        .paymentMethod(paymentMethod)
                        .build();

        order = foodOrderRepository.save(order);
        final Long orderId = order.getId();

        List<TFoodOrderItemEntity> savedItems = new ArrayList<>();
        List<CreateOrderRequest.OrderItemRequest> itemReqs = request.getItems();
        for (int i = 0; i < items.size(); i++) {
            TFoodOrderItemEntity item = items.get(i);
            item.setOrderId(orderId);
            TFoodOrderItemEntity savedItem = foodOrderItemRepository.save(item);
            savedItems.add(savedItem);

            CreateOrderRequest.OrderItemRequest itemReq = itemReqs.get(i);
            if (itemReq.getAddonIds() != null && !itemReq.getAddonIds().isEmpty()) {
                List<TMenuItemAddonEntity> selectedAddons =
                        menuItemAddonRepository.findAllById(itemReq.getAddonIds());
                for (TMenuItemAddonEntity addon : selectedAddons) {
                    foodOrderItemAddonRepository.save(
                            TFoodOrderItemAddonEntity.builder()
                                    .orderItemId(savedItem.getId())
                                    .addonId(addon.getId())
                                    .addonName(addon.getName())
                                    .addonPrice(addon.getExtraPrice())
                                    .build());
                }
            }
        }

        String qrImageUrl = null;
        if (paymentMethod == FoodOrderPaymentEnum.BANK_TRANSFER) {
            qrImageUrl = buildVietQrUrl(total, "Don hang " + order.getId());
        }

        OrderResponse response = toResponse(order, savedItems, qrImageUrl);
        messagingTemplate.convertAndSend("/topic/orders/new", response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse updateStatus(
            Long orderId, OrderStatusEnum status, Long confirmedByUserId) {
        TFoodOrderEntity order =
                foodOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đơn hàng không tồn tại: " + orderId));

        order.setStatus(status);

        // Ghi nhận ai xác nhận (lần đầu)
        if (confirmedByUserId != null && order.getConfirmedBy() == null) {
            order.setConfirmedBy(confirmedByUserId);
            order.setConfirmedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        }

        order = foodOrderRepository.save(order);

        // Tự động trừ kho khi xác nhận giao hàng
        if (status == OrderStatusEnum.DELIVERING) {
            List<TFoodOrderItemEntity> orderItems = foodOrderItemRepository.findByOrderId(orderId);
            validateStock(orderItems);
            deductInventory(orderItems, confirmedByUserId, orderId);
        }

        List<TFoodOrderItemEntity> items = foodOrderItemRepository.findByOrderId(orderId);
        OrderResponse response = toResponse(order, items, null);
        messagingTemplate.convertAndSend("/topic/orders/session/" + order.getSessionId(), response);
        return response;
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long orderId, String reason, Long cancelledByUserId) {
        TFoodOrderEntity order =
                foodOrderRepository
                        .findById(orderId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Đơn hàng không tồn tại: " + orderId));

        if (order.getStatus() == OrderStatusEnum.DONE
                || order.getStatus() == OrderStatusEnum.CANCELLED) {
            throw new IllegalStateException(
                    "Không thể hủy đơn hàng ở trạng thái: " + order.getStatus());
        }

        TUserEntity canceller =
                userRepository
                        .findById(cancelledByUserId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("Người dùng không tồn tại"));
        boolean isStaff =
                canceller.getRole() == com.netcoffee.enumtype.UserRoleEnum.ADMIN
                        || canceller.getRole() == com.netcoffee.enumtype.UserRoleEnum.STAFF;
        if (!isStaff && order.getStatus() != OrderStatusEnum.PENDING) {
            throw new IllegalStateException(
                    "Không thể hủy đơn khi nhân viên đã nhận — liên hệ nhân viên để được hỗ trợ");
        }

        order.setStatus(OrderStatusEnum.CANCELLED);
        order.setCancelReason(reason);
        if (order.getConfirmedBy() == null) {
            order.setConfirmedBy(cancelledByUserId);
            order.setConfirmedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        }

        order = foodOrderRepository.save(order);
        List<TFoodOrderItemEntity> items = foodOrderItemRepository.findByOrderId(orderId);
        OrderResponse response = toResponse(order, items, null);
        messagingTemplate.convertAndSend("/topic/orders/session/" + order.getSessionId(), response);
        return response;
    }

    private void validateStock(List<TFoodOrderItemEntity> orderItems) {
        for (TFoodOrderItemEntity item : orderItems) {
            List<TInventoryItemEntity> invItems =
                    inventoryItemRepository.findByMenuItemId(item.getItemId());
            for (TInventoryItemEntity inv : invItems) {
                if (inv.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalStateException(
                            "Không thể giao đơn: '" + inv.getName() + "' đã hết trong kho");
                }
                if (inv.getCurrentStock().compareTo(BigDecimal.valueOf(item.getQuantity())) < 0) {
                    throw new IllegalStateException(
                            "Không thể giao đơn: '"
                                    + inv.getName()
                                    + "' trong kho chỉ còn "
                                    + inv.getCurrentStock()
                                    + " "
                                    + inv.getUnit());
                }
            }
        }
    }

    private void deductInventory(
            List<TFoodOrderItemEntity> orderItems, Long performedBy, Long orderId) {
        for (TFoodOrderItemEntity item : orderItems) {
            List<TInventoryItemEntity> invItems =
                    inventoryItemRepository.findByMenuItemId(item.getItemId());
            for (TInventoryItemEntity inv : invItems) {
                BigDecimal qty = BigDecimal.valueOf(item.getQuantity());
                BigDecimal newStock = inv.getCurrentStock().subtract(qty);
                if (newStock.compareTo(BigDecimal.ZERO) < 0) newStock = BigDecimal.ZERO;
                inv.setCurrentStock(newStock);
                inventoryItemRepository.save(inv);

                inventoryTransactionRepository.save(
                        TInventoryTransactionEntity.builder()
                                .inventoryItemId(inv.getId())
                                .type(InventoryTransactionTypeEnum.EXPORT)
                                .quantity(qty)
                                .notes("Tự động từ đơn hàng #" + orderId)
                                .performedBy(performedBy != null ? performedBy : 1L)
                                .build());

                // Auto-disable menu item when stock hits 0
                boolean isOutOfStock = newStock.compareTo(BigDecimal.ZERO) <= 0;
                if (isOutOfStock && inv.getMenuItemId() != null) {
                    menuItemRepository
                            .findById(inv.getMenuItemId())
                            .ifPresent(
                                    mi -> {
                                        mi.setIsAvailable(false);
                                        menuItemRepository.save(mi);
                                    });
                }
                BigDecimal threshold =
                        inv.getMinStock().compareTo(BigDecimal.TEN) > 0
                                ? inv.getMinStock()
                                : BigDecimal.TEN;
                if (isOutOfStock || newStock.compareTo(threshold) < 0) {
                    messagingTemplate.convertAndSend(
                            "/topic/admin/inventory/low-stock",
                            Map.of(
                                    "inventoryItemId", inv.getId(),
                                    "inventoryItemName", inv.getName(),
                                    "currentStock", newStock,
                                    "unit", inv.getUnit(),
                                    "minStock", inv.getMinStock(),
                                    "outOfStock", isOutOfStock));
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findBySessionId(Long sessionId) {
        return foodOrderRepository.findBySessionIdOrderByCreatedAtDesc(sessionId).stream()
                .map(o -> toResponse(o, foodOrderItemRepository.findByOrderId(o.getId()), null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findPendingOrders() {
        return foodOrderRepository.findByStatus(OrderStatusEnum.PENDING).stream()
                .map(o -> toResponse(o, foodOrderItemRepository.findByOrderId(o.getId()), null))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return foodOrderRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(o -> toResponse(o, foodOrderItemRepository.findByOrderId(o.getId()), null))
                .toList();
    }

    private String buildVietQrUrl(BigDecimal amount, String description) {
        String encodedName = URLEncoder.encode(vietQr.accountName(), StandardCharsets.UTF_8);
        String encodedDesc = URLEncoder.encode(description, StandardCharsets.UTF_8);
        return String.format(
                "https://img.vietqr.io/image/%s-%s-compact2.png?amount=%s&addInfo=%s&accountName=%s",
                vietQr.bankBin(),
                vietQr.accountNumber(),
                amount.toPlainString(),
                encodedDesc,
                encodedName);
    }

    private OrderResponse toResponse(
            TFoodOrderEntity order, List<TFoodOrderItemEntity> items, String qrImageUrl) {
        String confirmedByName = null;
        if (order.getConfirmedBy() != null) {
            confirmedByName =
                    userRepository
                            .findById(order.getConfirmedBy())
                            .map(
                                    u ->
                                            u.getFullName() != null
                                                    ? u.getFullName()
                                                    : u.getPhoneNumber())
                            .orElse(null);
        }

        List<OrderResponse.OrderItemResponse> itemResponses =
                items.stream()
                        .map(
                                i -> {
                                    List<OrderResponse.AddonResponse> addonResponses =
                                            foodOrderItemAddonRepository
                                                    .findByOrderItemId(i.getId())
                                                    .stream()
                                                    .map(
                                                            a ->
                                                                    OrderResponse.AddonResponse
                                                                            .builder()
                                                                            .addonId(a.getAddonId())
                                                                            .addonName(
                                                                                    a
                                                                                            .getAddonName())
                                                                            .addonPrice(
                                                                                    a
                                                                                            .getAddonPrice())
                                                                            .build())
                                                    .toList();

                                    BigDecimal addonTotal =
                                            addonResponses.stream()
                                                    .map(OrderResponse.AddonResponse::getAddonPrice)
                                                    .reduce(BigDecimal.ZERO, BigDecimal::add);

                                    String itemName = null;
                                    try {
                                        itemName =
                                                menuItemService.findById(i.getItemId()).getName();
                                    } catch (Exception ignored) {
                                    }

                                    return OrderResponse.OrderItemResponse.builder()
                                            .menuItemId(i.getItemId())
                                            .menuItemName(itemName)
                                            .quantity(i.getQuantity())
                                            .unitPrice(i.getUnitPrice().add(addonTotal))
                                            .subtotal(
                                                    i.getUnitPrice()
                                                            .add(addonTotal)
                                                            .multiply(
                                                                    BigDecimal.valueOf(
                                                                            i.getQuantity())))
                                            .addons(addonResponses)
                                            .build();
                                })
                        .toList();

        return OrderResponse.builder()
                .id(order.getId())
                .sessionId(order.getSessionId())
                .machineId(order.getMachineId())
                .status(order.getStatus())
                .totalPrice(order.getTotalPrice())
                .note(order.getNote())
                .confirmedBy(order.getConfirmedBy())
                .confirmedByName(confirmedByName)
                .confirmedAt(order.getConfirmedAt())
                .cancelReason(order.getCancelReason())
                .paymentMethod(order.getPaymentMethod())
                .qrImageUrl(qrImageUrl)
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
