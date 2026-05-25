package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {

    @NotBlank(message = "Mật khẩu mới không được trống")
    @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String newPassword;
}
