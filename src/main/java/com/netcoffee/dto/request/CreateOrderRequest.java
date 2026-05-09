package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class CreateOrderRequest
{

    @NotNull
    private Long sessionId;

    @NotNull
    private Long machineId;

    @NotEmpty(message = "Đơn hàng phải có ít nhất 1 món")
    private List<OrderItemRequest> items;

    private String note;

    @Getter @Setter
    public static class OrderItemRequest
    {
        @NotNull
        private Long menuItemId;

        @NotNull
        private Integer quantity;
    }
}
