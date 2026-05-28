package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter @Builder
public class MenuItemResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private String category;
    private String imageUrl;
    private Boolean isAvailable;
    private List<AddonResponse> addons;

    @Getter @Builder
    public static class AddonResponse {
        private Long id;
        private String name;
        private BigDecimal extraPrice;
        private Boolean isAvailable;
    }
}
