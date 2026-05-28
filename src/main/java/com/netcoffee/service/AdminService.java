package com.netcoffee.service;

import com.netcoffee.dto.request.AdminDeductRequest;
import com.netcoffee.dto.response.AdminTransactionResponse;
import com.netcoffee.dto.response.SessionHistoryResponse;
import com.netcoffee.dto.response.UserResponse;
import java.util.List;
import org.springframework.data.domain.Page;

public interface AdminService {

    UserResponse deductBalance(Long userId, AdminDeductRequest request, Long adminId);

    Page<AdminTransactionResponse> getTransactions(int page, int size);

    Page<SessionHistoryResponse> getSessionHistory(int page, int size);

    List<UserResponse> searchUsers(String phone, boolean isAdmin);
}
