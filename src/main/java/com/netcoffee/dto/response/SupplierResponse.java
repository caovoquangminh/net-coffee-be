package com.netcoffee.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SupplierResponse {
    private Long id;
    private String name;
    private String phone;
    private String address;
    private String note;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
