package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

    @NotBlank(message = "Số điện thoại không được trống")
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu không được trống")
    private String password;

    /**
     * ID máy tính đang đăng nhập — dùng để tạo session tự động.
     * Bắt buộc khi đăng nhập từ client app.
     */
    @NotNull(message = "Machine ID không được trống")
    private Long machineId;
}