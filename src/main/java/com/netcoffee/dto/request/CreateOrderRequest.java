package com.netcoffee.dto.request;

import com.netcoffee.enumtype.FoodOrderPaymentEnum;
import jakarta.validation.constraints.Min;
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

    private FoodOrderPaymentEnum paymentMethod;

    @Getter @Setter
    public static class OrderItemRequest
    {
        @NotNull
        private Long menuItemId;

        @NotNull
        @Min(value = 1, message = "Số lượng phải ít nhất là 1")
        private Integer quantity;

        /** Danh sách addon ID đã chọn (tuỳ chọn) */
        private List<Long> addonIds;
    }
}
