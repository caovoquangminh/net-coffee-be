package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.TransactionResponse;
import com.netcoffee.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.TRANSACTIONS)
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<Page<TransactionResponse>>> getMyTransactions(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "" + AppConstant.DEFAULT_PAGE_SIZE) int size) {
        Long userId = Long.parseLong(userDetails.getUsername());
        size = Math.min(size, AppConstant.MAX_PAGE_SIZE);
        Pageable pageable = PageRequest.of(page, size);
        return ResponseEntity.ok(ApiResponse.ok(transactionService.findByUserId(userId, pageable)));
    }
}
