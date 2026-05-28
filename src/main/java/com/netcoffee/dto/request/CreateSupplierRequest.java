package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class CreateSupplierRequest {

    @NotBlank(message = "Tên nhà cung cấp không được trống")
    @Size(max = 150)
    private String name;

    @NotBlank(message = "Số điện thoại không được trống")
    @Pattern(regexp = "^[0-9]{9,15}$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Size(max = 300)
    private String address;

    @Size(max = 500)
    private String note;
}
