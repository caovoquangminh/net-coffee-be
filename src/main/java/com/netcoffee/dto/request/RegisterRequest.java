package com.netcoffee.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class RegisterRequest
{

    @NotBlank(message = "Số điện thoại không được trống") @Pattern(regexp = "^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ")
    private String phoneNumber;

    @NotBlank(message = "Mật khẩu không được trống") @Size(min = 6, message = "Mật khẩu tối thiểu 6 ký tự")
    private String password;

    private String fullName;
}
