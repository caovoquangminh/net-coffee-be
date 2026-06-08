package com.netcoffee.service;

import com.netcoffee.dto.request.ChangePasswordRequest;
import com.netcoffee.dto.request.CreateCustomerRequest;
import com.netcoffee.dto.request.ResetPasswordRequest;
import com.netcoffee.dto.request.UpdateProfileRequest;
import com.netcoffee.dto.request.UpdateStaffProfileRequest;
import com.netcoffee.dto.request.UpdateUserRequest;
import com.netcoffee.dto.response.UserResponse;

public interface UserManagementService {

    /** Admin cập nhật thông tin user (họ tên, kích hoạt). Không đổi SĐT. */
    UserResponse adminUpdateUser(Long userId, UpdateUserRequest request);

    /** Admin đặt lại mật khẩu cho user bất kỳ. */
    void adminResetPassword(Long userId, ResetPasswordRequest request);

    /** Admin cập nhật hồ sơ nhân viên (địa chỉ, CCCD, email, lương giờ, v.v.). */
    UserResponse updateStaffProfile(Long userId, UpdateStaffProfileRequest request);

    /** Admin xóa mềm user (set deletedAt = now, isActive = false). */
    void softDeleteUser(Long userId, Long adminId);

    /** User tự cập nhật profile (họ tên). Không đổi SĐT. */
    UserResponse updateMyProfile(Long userId, UpdateProfileRequest request);

    /** User tự đổi mật khẩu sau khi xác thực mật khẩu cũ. */
    void changeMyPassword(Long userId, ChangePasswordRequest request);

    /** Admin tạo tài khoản hội viên mới (role = CUSTOMER). */
    UserResponse createCustomer(CreateCustomerRequest request);

    /** Tìm tài khoản hội viên (CUSTOMER) theo số điện thoại (tối thiểu 3 ký tự). */
    java.util.List<UserResponse> searchCustomers(String phone);
}
