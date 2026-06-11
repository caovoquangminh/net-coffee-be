package com.netcoffee.dto.request;

import com.netcoffee.enumtype.UserRoleEnum;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {

    @Size(max = 100, message = "Họ tên tối đa 100 ký tự")
    private String fullName;

    /** Cho phép admin đổi số điện thoại (đăng nhập) của tài khoản. */
    @Size(min = 9, max = 15, message = "Số điện thoại phải từ 9 đến 15 ký tự")
    private String phoneNumber;

    private Boolean isActive;

    /** Chỉ cho phép STAFF hoặc CUSTOMER — không thể đổi thành ADMIN qua endpoint này. */
    private UserRoleEnum role;
}
