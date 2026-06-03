package com.netcoffee.controller;

import com.netcoffee.constant.ApiPaths;
import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.DashboardOwnerResponse;
import com.netcoffee.dto.response.DashboardStatsResponse;
import com.netcoffee.service.DashboardOwnerService;
import com.netcoffee.service.DashboardService;
import com.netcoffee.service.impl.MonthlyReportServiceImpl;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import lombok.RequiredArgsConstructor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiPaths.DASHBOARD)
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardOwnerService dashboardOwnerService;
    private final MonthlyReportServiceImpl monthlyReportService;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        return ResponseEntity.ok(ApiResponse.ok(dashboardService.getStats()));
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardOwnerResponse>> getOwnerStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate date) {
        LocalDate target = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok(dashboardOwnerService.getOwnerStats(target)));
    }

    @GetMapping("/monthly-report")
    @PreAuthorize("hasRole('ADMIN')")
    public void downloadMonthlyReport(
            @RequestParam(required = false) String yearMonth, HttpServletResponse response)
            throws IOException {

        YearMonth ym;
        try {
            ym = yearMonth != null ? YearMonth.parse(yearMonth) : YearMonth.now();
        } catch (DateTimeParseException e) {
            ym = YearMonth.now();
        }

        String filename = "bao-cao-" + ym + ".xlsx";
        response.setContentType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + filename + "\"");

        try (XSSFWorkbook wb = monthlyReportService.generateMonthlyReport(ym)) {
            wb.write(response.getOutputStream());
        }
    }
}
