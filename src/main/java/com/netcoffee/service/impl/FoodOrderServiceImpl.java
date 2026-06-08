package com.netcoffee.service.impl;

import com.netcoffee.config.VietQrProperties;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.request.CreateOrderRequest;
import com.netcoffee.dto.response.OrderResponse;
import com.netcoffee.entity.*;
import com.netcoffee.enumtype.FoodOrderPaymentEnum;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.enumtype.SessionStatusEnum;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FoodOrderServiceImpl implements FoodOrderService {

    private final FoodOrderRepository foodOrderRepository;
    private final FoodOrderItemRepository foodOrderItemRepository;
    private final FoodOrderItemAddonRepository foodOrderItemAddonRepository;
    private final MenuItemAddonRepository menuItemAddonRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final MenuItemInventoryRepository menuItemInventoryRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final MenuItemService menuItemService;
    private final MenuItemRepository menuItemRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final VietQrProperties vietQr;

    @Override
    @Transactional
    public OrderResponse createOrder(Long userId, CreateOrderRequest request) {
        TSessionEntity session =
                sessionRepository
                        .findById(request.getSessionId())
                        .orElseThrow(() -> new ResourceNotFoundException("Session không tồn tại"));
        if (session.getStatus() != SessionStatusEnum.ACTIVE) {
            throw new IllegalStateException("Phiên làm việc đã kết thúc, không thể đặt món");
        }
        if (!session.getUserId().equals(userId)) {
            throw new AccessDeniedException("Không thể đặt món cho phiên của người khác");
        }

        List<TFoodOrderItemEntity> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CreateOrderRequest.OrderItemRequest itemReq : request.getItems()) {
            TMenuItemEntity menuItem = menuItemService.findById(itemReq.getMenuItemId());
            if (!menuItem.getIsAvailable()) {
                throw new IllegalArgumentException(
                        "Món '" + menuItem.getName() + "' hiện không có sẵn");
            }
            // Block order if any linked ingredient is actually out of stock (flag may be stale)
            for (TMenuItemInventoryEntity link :
                    menuItemInventoryRepository.findByMenuItemId(menuItem.getId())) {
                TInventoryItemEntity inv =
                        inventoryItemRepository.findById(link.getInventoryItemId()).orElse(null);
                if (inv != null && inv.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
                    throw new IllegalArgumentException(
                            "Món '"
                                    + menuItem.getName()
                                    + "' đang hết nguyên liệu '"
                                    + inv.getName()
                                    + "'");
                }
            }

            BigDecimal addonTotal = BigDecimal.ZERO;
            if (itemReq.getAddonIds() != null && !itemReq.getAddonIds().isEmpty()) {
                List<TMenuItemAddonEntity> selectedAddons =
                        menuItemAddonRepository.findAllById(itemReq.getAddonIds());
                for (TMenuItemAddonEntity addon : selectedAddons) {
                    if (!addon.getIsAvailable()) {
                        throw new IllegalArgumentException(
                                "Topping '" + addon.getName() + "' hiện không có sẵn");
                    }
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

        boolean paymentVerified = paymentMethod != FoodOrderPaymentEnum.BANK_TRANSFER;

        TFoodOrderEntity order =
                TFoodOrderEntity.builder()
                        .sessionId(request.getSessionId())
                        .userId(userId)
                        .machineId(request.getMachineId())
                        .status(OrderStatusEnum.PENDING)
                        .totalPrice(total)
                        .note(request.getNote())
                        .paymentMethod(paymentMethod)
                        .paymentVerified(paymentVerified)
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

        OrderStatusEnum current = order.getStatus();
        boolean validTransition =
                (current == OrderStatusEnum.PENDING && status == OrderStatusEnum.DELIVERING)
                        || (current == OrderStatusEnum.PENDING
                                && status == OrderStatusEnum.CANCELLED)
                        || (current == OrderStatusEnum.DELIVERING && status == OrderStatusEnum.DONE)
                        || (current == OrderStatusEnum.DELIVERING
                                && status == OrderStatusEnum.CANCELLED);
        if (!validTransition) {
            throw new IllegalStateException(
                    "Không thể chuyển trạng thái đơn hàng từ " + current + " sang " + status);
        }

        if (status == OrderStatusEnum.DELIVERING
                && order.getPaymentMethod() == FoodOrderPaymentEnum.BANK_TRANSFER
                && !Boolean.TRUE.equals(order.getPaymentVerified())) {
            throw new IllegalStateException(
                    "Đơn hàng chuyển khoản chưa được xác nhận thanh toán — "
                            + "nhân viên cần kiểm tra tài khoản ngân hàng và bấm \"Xác nhận đã nhận tiền\" trước");
        }

        order.setStatus(status);

        if (confirmedByUserId != null && order.getConfirmedBy() == null) {
            order.setConfirmedBy(confirmedByUserId);
            order.setConfirmedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        }

        order = foodOrderRepository.save(order);

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
    public OrderResponse confirmPayment(Long orderId, Long staffId) {
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
                    "Không thể xác nhận thanh toán cho đơn ở trạng thái: " + order.getStatus());
        }
        if (order.getPaymentMethod() != FoodOrderPaymentEnum.BANK_TRANSFER) {
            throw new IllegalStateException("Chỉ đơn chuyển khoản mới cần xác nhận thanh toán");
        }

        order.setPaymentVerified(true);
        if (order.getConfirmedBy() == null) {
            order.setConfirmedBy(staffId);
            order.setConfirmedAt(LocalDateTime.now(AppConstant.VN_ZONE));
        }

        order = foodOrderRepository.save(order);
        List<TFoodOrderItemEntity> items = foodOrderItemRepository.findByOrderId(orderId);
        OrderResponse response = toResponse(order, items, null);
        messagingTemplate.convertAndSend("/topic/orders/session/" + order.getSessionId(), response);
        return response;
    }

    @Override
    @Transactional
    public void autoConfirmPaymentByWebhook(String content, BigDecimal amount) {
        if (content == null || amount == null) return;

        java.util.regex.Pattern p =
                java.util.regex.Pattern.compile("(?i)(?:don\\s*hang|DH)\\s*(\\d+)");
        java.util.regex.Matcher m = p.matcher(content);
        if (!m.find()) return;

        long orderId;
        try {
            orderId = Long.parseLong(m.group(1));
        } catch (NumberFormatException e) {
            return;
        }

        foodOrderRepository
                .findById(orderId)
                .filter(o -> o.getPaymentMethod() == FoodOrderPaymentEnum.BANK_TRANSFER)
                .filter(o -> !o.getPaymentVerified())
                .filter(
                        o ->
                                o.getStatus() != OrderStatusEnum.DONE
                                        && o.getStatus() != OrderStatusEnum.CANCELLED)
                .filter(o -> o.getTotalPrice().compareTo(amount) == 0)
                .ifPresent(
                        o -> {
                            o.setPaymentVerified(true);
                            TFoodOrderEntity saved = foodOrderRepository.save(o);
                            List<TFoodOrderItemEntity> savedItems =
                                    foodOrderItemRepository.findByOrderId(saved.getId());
                            OrderResponse resp = toResponse(saved, savedItems, null);
                            messagingTemplate.convertAndSend(
                                    "/topic/orders/session/" + saved.getSessionId(), resp);
                            messagingTemplate.convertAndSend(
                                    "/topic/admin/payment/verified",
                                    Map.of(
                                            "orderId", saved.getId(),
                                            "amount", amount,
                                            "sessionId", saved.getSessionId()));
                        });
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
            List<TMenuItemInventoryEntity> links =
                    menuItemInventoryRepository.findByMenuItemId(item.getItemId());
            for (TMenuItemInventoryEntity link : links) {
                BigDecimal needed =
                        link.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity()));
                inventoryItemRepository
                        .findById(link.getInventoryItemId())
                        .ifPresent(
                                inv -> {
                                    if (inv.getCurrentStock().compareTo(needed) < 0) {
                                        throw new IllegalStateException(
                                                "Không thể giao đơn: '"
                                                        + inv.getName()
                                                        + "' cần "
                                                        + needed
                                                        + " "
                                                        + inv.getUnit()
                                                        + " nhưng kho chỉ còn "
                                                        + inv.getCurrentStock()
                                                        + " "
                                                        + inv.getUnit());
                                    }
                                });
            }

            List<TFoodOrderItemAddonEntity> itemAddons =
                    foodOrderItemAddonRepository.findByOrderItemId(item.getId());
            for (TFoodOrderItemAddonEntity itemAddon : itemAddons) {
                menuItemAddonRepository
                        .findById(itemAddon.getAddonId())
                        .filter(addon -> addon.getInventoryItemId() != null)
                        .flatMap(
                                addon ->
                                        inventoryItemRepository.findById(
                                                addon.getInventoryItemId()))
                        .ifPresent(
                                inv -> {
                                    if (inv.getCurrentStock().compareTo(BigDecimal.ZERO) <= 0) {
                                        throw new IllegalStateException(
                                                "Không thể giao đơn: topping '"
                                                        + itemAddon.getAddonName()
                                                        + "' đã hết trong kho");
                                    }
                                    if (inv.getCurrentStock()
                                                    .compareTo(
                                                            BigDecimal.valueOf(item.getQuantity()))
                                            < 0) {
                                        throw new IllegalStateException(
                                                "Không thể giao đơn: topping '"
                                                        + itemAddon.getAddonName()
                                                        + "' trong kho chỉ còn "
                                                        + inv.getCurrentStock()
                                                        + " "
                                                        + inv.getUnit());
                                    }
                                });
            }
        }
    }

    private void deductInventory(
            List<TFoodOrderItemEntity> orderItems, Long performedBy, Long orderId) {
        for (TFoodOrderItemEntity item : orderItems) {
            List<TMenuItemInventoryEntity> links =
                    menuItemInventoryRepository.findByMenuItemId(item.getItemId());
            for (TMenuItemInventoryEntity link : links) {
                BigDecimal needed =
                        link.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity()));
                inventoryItemRepository
                        .findById(link.getInventoryItemId())
                        .ifPresent(
                                inv -> deductFromInventory(inv, needed, performedBy, orderId, ""));
            }

            List<TFoodOrderItemAddonEntity> itemAddons =
                    foodOrderItemAddonRepository.findByOrderItemId(item.getId());
            for (TFoodOrderItemAddonEntity itemAddon : itemAddons) {
                menuItemAddonRepository
                        .findById(itemAddon.getAddonId())
                        .filter(addon -> addon.getInventoryItemId() != null)
                        .flatMap(
                                addon ->
                                        inventoryItemRepository.findById(
                                                addon.getInventoryItemId()))
                        .ifPresent(
                                inv ->
                                        deductFromInventory(
                                                inv,
                                                BigDecimal.valueOf(item.getQuantity()),
                                                performedBy,
                                                orderId,
                                                " (topping: " + itemAddon.getAddonName() + ")"));
            }
        }
    }

    private void deductFromInventory(
            TInventoryItemEntity inv,
            BigDecimal quantity,
            Long performedBy,
            Long orderId,
            String noteSuffix) {
        BigDecimal qty = quantity;
        BigDecimal actualDeducted = qty.min(inv.getCurrentStock());
        BigDecimal newStock = inv.getCurrentStock().subtract(actualDeducted);
        inv.setCurrentStock(newStock);
        inventoryItemRepository.save(inv);

        inventoryTransactionRepository.save(
                TInventoryTransactionEntity.builder()
                        .inventoryItemId(inv.getId())
                        .type(InventoryTransactionTypeEnum.EXPORT)
                        .quantity(actualDeducted)
                        .notes("Tự động từ đơn hàng #" + orderId + noteSuffix)
                        .performedBy(performedBy != null ? performedBy : 1L)
                        .build());

        boolean isOutOfStock = newStock.compareTo(BigDecimal.ZERO) <= 0;

        if (isOutOfStock) {
            List<Long> disabledMenuItemIds = new ArrayList<>();
            menuItemInventoryRepository
                    .findByInventoryItemId(inv.getId())
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
                    .findByInventoryItemId(inv.getId())
                    .forEach(
                            addon -> {
                                addon.setIsAvailable(false);
                                addon.setDisabledByStock(true);
                                menuItemAddonRepository.save(addon);
                            });

            if (!disabledMenuItemIds.isEmpty()) {
                messagingTemplate.convertAndSend(
                        "/topic/menu-updated",
                        Map.of("action", "DISABLED", "menuItemIds", disabledMenuItemIds));
            }
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
        return foodOrderRepository.findAllByOrderByCreatedAtDesc().stream()
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
                                    } catch (Exception e) {
                                        log.warn(
                                                "Menu item {} not found when building order response: {}",
                                                i.getItemId(),
                                                e.getMessage());
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
                .paymentVerified(order.getPaymentVerified())
                .qrImageUrl(qrImageUrl)
                .createdAt(order.getCreatedAt())
                .items(itemResponses)
                .build();
    }
}
