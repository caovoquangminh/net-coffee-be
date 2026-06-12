package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.service.AppSettingService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Cấu hình hệ thống cho admin: Telegram group chat + tham số thưởng/phạt. */
@RestController
@RequestMapping(ApiPaths.SETTINGS)
@RequiredArgsConstructor
public class SettingsController {

    private final AppSettingService appSettingService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(appSettingService.getAll()));
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, String>>> update(
            @RequestBody Map<String, String> settings) {
        settings.forEach(appSettingService::set);
        return ResponseEntity.ok(ApiResponse.ok("Đã lưu cấu hình", appSettingService.getAll()));
    }
}
