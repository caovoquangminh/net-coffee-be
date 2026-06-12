package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.AttendanceRecordResponse;
import com.netcoffee.dto.response.OvertimeRequestResponse;
import com.netcoffee.dto.response.ShiftResponse;
import com.netcoffee.enumtype.OvertimeTypeEnum;
import com.netcoffee.service.OvertimeService;
import com.netcoffee.service.ShiftService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiPaths.ATTENDANCE)
@RequiredArgsConstructor
public class AttendanceController {

    private final ShiftService shiftService;
    private final OvertimeService overtimeService;

    @GetMapping("/shifts")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<ShiftResponse>>> getShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getShiftsForDateRange(from, to)));
    }

    @PostMapping("/shifts/generate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> generateShifts(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        shiftService.generateShiftsForDate(date);
        return ResponseEntity.ok(ApiResponse.ok("Đã tạo ca cho ngày " + date, null));
    }

    @PostMapping("/shifts/{id}/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<ShiftResponse>> registerShift(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok("Đăng ký ca thành công", shiftService.registerShift(userId, id)));
    }

    @DeleteMapping("/shifts/{id}/register")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Void>> cancelRegistration(
            @PathVariable Long id,
            @RequestParam Long registrationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        boolean isAdmin =
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        shiftService.cancelRegistration(registrationId, userId, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Hủy đăng ký ca thành công", null));
    }

    @GetMapping("/registration-window")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<Boolean>> registrationWindow() {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.isRegistrationWindowOpen()));
    }

    @PostMapping("/shifts/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<ShiftResponse>> assignShift(
            @PathVariable Long id, @RequestBody Map<String, Long> body) {
        Long userId = body.get("userId");
        return ResponseEntity.ok(
                ApiResponse.ok("Đã sắp ca cho nhân viên", shiftService.assignShift(id, userId)));
    }

    @PostMapping("/checkin")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> checkIn(
            @RequestBody Map<String, Long> body, @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long shiftId = body.get("shiftId");
        return ResponseEntity.ok(
                ApiResponse.ok("Check-in thành công", shiftService.checkIn(userId, shiftId)));
    }

    @PostMapping("/checkout")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<AttendanceRecordResponse>> checkOut(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long shiftId = Long.valueOf(body.get("shiftId").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Check-out thành công", shiftService.checkOut(userId, shiftId, reason)));
    }

    @GetMapping("/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getHistory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long userId,
            @AuthenticationPrincipal UserDetails userDetails) {

        boolean isAdmin =
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long currentUserId = Long.parseLong(userDetails.getUsername());

        Long targetUserId = isAdmin ? userId : currentUserId;
        return ResponseEntity.ok(
                ApiResponse.ok(shiftService.getAttendanceHistory(targetUserId, from, to)));
    }

    @GetMapping("/current")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<AttendanceRecordResponse>>> getCurrentOnShift() {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getCurrentOnShift()));
    }

    @GetMapping("/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<com.netcoffee.dto.response.AttendanceDashboardResponse>>
            getDashboard() {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getDashboardSummary()));
    }

    @GetMapping("/colleagues")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<com.netcoffee.dto.response.StaffOptionResponse>>>
            getColleagues() {
        return ResponseEntity.ok(ApiResponse.ok(shiftService.getColleagues()));
    }

    @GetMapping("/overtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<List<OvertimeRequestResponse>>> getOvertime(
            @AuthenticationPrincipal UserDetails userDetails) {
        boolean isAdmin =
                userDetails.getAuthorities().stream()
                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        Long userId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(overtimeService.getOvertimeRequests(userId, isAdmin)));
    }

    @PostMapping("/overtime")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<ApiResponse<OvertimeRequestResponse>> createOvertime(
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        Long shiftId = Long.valueOf(body.get("shiftId").toString());
        String reason = body.get("reason") != null ? body.get("reason").toString() : null;
        OvertimeTypeEnum type = OvertimeTypeEnum.valueOf(body.get("otType").toString());
        Long coveringUserId =
                body.get("coveringUserId") != null
                        ? Long.valueOf(body.get("coveringUserId").toString())
                        : null;
        Long replacementUserId =
                body.get("replacementUserId") != null
                        ? Long.valueOf(body.get("replacementUserId").toString())
                        : null;
        java.time.LocalDateTime otStart =
                body.get("otStartTime") != null
                        ? java.time.LocalDateTime.parse(body.get("otStartTime").toString())
                        : null;
        java.time.LocalDateTime otEnd =
                body.get("otEndTime") != null
                        ? java.time.LocalDateTime.parse(body.get("otEndTime").toString())
                        : null;
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Tạo yêu cầu OT thành công",
                        overtimeService.createOvertimeRequest(
                                userId,
                                shiftId,
                                reason,
                                type,
                                coveringUserId,
                                otStart,
                                otEnd,
                                replacementUserId)));
    }

    @PatchMapping("/overtime/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeRequestResponse>> approveOvertime(
            @PathVariable Long id, @AuthenticationPrincipal UserDetails userDetails) {
        Long adminId = Long.parseLong(userDetails.getUsername());
        return ResponseEntity.ok(
                ApiResponse.ok(
                        "Phê duyệt OT thành công", overtimeService.approveOvertime(id, adminId)));
    }

    @PatchMapping("/overtime/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<OvertimeRequestResponse>> rejectOvertime(
            @PathVariable Long id) {
        return ResponseEntity.ok(
                ApiResponse.ok("Từ chối OT thành công", overtimeService.rejectOvertime(id)));
    }
}
