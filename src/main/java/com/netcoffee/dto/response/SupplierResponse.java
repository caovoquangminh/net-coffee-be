package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter @Builder
public class SupplierResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String note;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
