package com.netcoffee.dto.response;

import com.netcoffee.enumtype.OrderStatusEnum;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Builder
public class OrderResponse
{

    private Long id;
    private Long sessionId;
    private Long machineId;
    private OrderStatusEnum status;
    private BigDecimal totalPrice;
    private String note;
    private LocalDateTime createdAt;
    private List<OrderItemResponse> items;

    @Getter @Builder
    public static class OrderItemResponse
    {
        private Long menuItemId;
        private String menuItemName;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }
}
