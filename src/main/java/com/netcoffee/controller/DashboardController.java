package com.netcoffee.controller;

import com.netcoffee.dto.response.ApiResponse;
import com.netcoffee.dto.response.DashboardStatsResponse;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.repository.FoodOrderRepository;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionRepository transactionRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final MachineRepository machineRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);

        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        BigDecimal todayNet = transactionRepository.sumByTypeBetween(TransactionTypeEnum.DEDUCT, todayStart, todayEnd);
        BigDecimal todayFood = foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, todayStart, todayEnd);

        BigDecimal monthNet = transactionRepository.sumByTypeBetween(TransactionTypeEnum.DEDUCT, monthStart, monthEnd);
        BigDecimal monthFood = foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, monthStart, monthEnd);

        long total = machineRepository.count();
        long inUse = machineRepository.countByStatus(MachineStatusEnum.IN_USE);
        long available = machineRepository.countByStatus(MachineStatusEnum.AVAILABLE);
        DashboardStatsResponse stats = DashboardStatsResponse.builder()
                .todayNetRevenue(todayNet)
                .todayFoodRevenue(todayFood)
                .todayTotalRevenue(todayNet.add(todayFood))
                .monthNetRevenue(monthNet)
                .monthFoodRevenue(monthFood)
                .monthTotalRevenue(monthNet.add(monthFood))
                .totalMachines(total)
                .machinesInUse(inUse)
                .machinesAvailable(available)
                .activeSessions(inUse)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(stats));
    }
}
