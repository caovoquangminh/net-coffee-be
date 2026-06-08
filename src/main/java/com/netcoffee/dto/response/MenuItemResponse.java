package com.netcoffee.dto.response;

import java.math.BigDecimal;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuItemResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private String description;
    private Boolean isAvailable;
    private List<AddonResponse> addons;
    private List<InventoryLinkResponse> linkedInventoryItems;

    @Getter
    @Builder
    public static class AddonResponse {
        private Long id;
        private String name;
        private BigDecimal extraPrice;
        private Boolean isAvailable;
        private Long inventoryItemId;
    }

    @Getter
    @Builder
    public static class InventoryLinkResponse {
        private Long inventoryItemId;
        private String inventoryItemName;
        private BigDecimal quantity;
    }
}
