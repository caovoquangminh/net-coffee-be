package com.netcoffee.dto.response;

import com.netcoffee.enumtype.FoodOrderPaymentEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class OrderResponse {

    private Long id;
    private Long sessionId;
    private Long machineId;
    private OrderStatusEnum status;
    private BigDecimal totalPrice;
    private String note;
    private Long confirmedBy;
    private String confirmedByName;
    private LocalDateTime confirmedAt;
    private String cancelReason;
    private FoodOrderPaymentEnum paymentMethod;
    private Boolean paymentVerified;
    private String qrImageUrl;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    @Getter
    @Builder
    public static class OrderItemResponse {
        private Long menuItemId;
        private String menuItemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
        private List<AddonResponse> addons;
    }

    @Getter
    @Builder
    public static class AddonResponse {
        private Long addonId;
        private String addonName;
        private BigDecimal addonPrice;
    }
}
