package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.PayrollPeriodResponse;
import com.netcoffee.dto.response.PayrollRecordResponse;
import com.netcoffee.service.PayrollService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.PAYROLL)
@RequiredArgsConstructor
public class PayrollController {

    private final PayrollService payrollService;

    @GetMapping("/periods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollPeriodResponse>>> getPeriods() {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllPeriods()));
    }

    @PostMapping("/periods")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> createPeriod(
            @RequestBody Map<String, Integer> body) {
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Tạo kỳ lương thành công",
                        payrollService.getOrCreatePeriod(body.get("year"), body.get("month"))));
    }

    @PostMapping("/periods/{id}/calculate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollRecordResponse>>> calculate(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Tính lương thành công", payrollService.calculatePayroll(id)));
    }

    @PostMapping("/periods/{id}/send")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollPeriodResponse>> send(@PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Đã gửi bảng lương cho nhân viên", payrollService.sendPayroll(id)));
    }

    @GetMapping("/periods/{id}/records")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<PayrollRecordResponse>>> getPeriodRecords(
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getAllPayroll(id)));
    }

    @PutMapping("/records/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> updateManualFields(
            @PathVariable Long id, @RequestBody Map<String, Object> body) {
        BigDecimal bonus = parseBigDecimal(body.get("bonus"));
        BigDecimal penalty = parseBigDecimal(body.get("penalty"));
        BigDecimal responsibility = parseBigDecimal(body.get("responsibility"));
        BigDecimal advance = parseBigDecimal(body.get("advance"));
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Cập nhật thành công",
                        payrollService.updateManualFields(
                                id, bonus, penalty, responsibility, advance)));
    }

    @PostMapping("/records/{id}/confirm")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> confirm(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Đã xác nhận lương", payrollService.confirmPayroll(id, userId)));
    }

    @PostMapping("/records/{id}/dispute")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<PayrollRecordResponse>> dispute(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        String reason = body.get("reason");
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Đã phản hồi lương", payrollService.disputePayroll(id, userId, reason)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<PayrollRecordResponse>>> getMyPayroll(
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.ok(payrollService.getMyPayroll(userId)));
    }

    private BigDecimal parseBigDecimal(Object value) {
        if (value == null) return null;
        return new BigDecimal(value.toString());
    }
}
