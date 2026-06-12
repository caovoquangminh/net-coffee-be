package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

/** Tùy chọn nhân viên tối giản (id + tên) để chọn người làm thay / sắp ca. */
@Getter
@Builder
public class StaffOptionResponse {
    private Long id;
    private String name;
}
