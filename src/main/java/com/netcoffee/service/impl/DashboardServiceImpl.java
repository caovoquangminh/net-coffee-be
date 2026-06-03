package com.netcoffee.service.impl;

import com.netcoffee.constant.AppConstant;
import com.netcoffee.dto.response.DashboardStatsResponse;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.repository.FoodOrderRepository;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.service.DashboardService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final TransactionRepository transactionRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final MachineRepository machineRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardStatsResponse getStats() {
        LocalDate today = LocalDate.now(AppConstant.VN_ZONE);
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = todayStart.plusDays(1);
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = monthStart.plusMonths(1);

        BigDecimal todayNet =
                transactionRepository.sumByTypeBetween(
                        TransactionTypeEnum.DEDUCT, todayStart, todayEnd);
        BigDecimal todayFood =
                foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, todayStart, todayEnd);
        BigDecimal todayTopUp =
                transactionRepository.sumByTypeBetween(
                        TransactionTypeEnum.TOPUP, todayStart, todayEnd);

        BigDecimal monthNet =
                transactionRepository.sumByTypeBetween(
                        TransactionTypeEnum.DEDUCT, monthStart, monthEnd);
        BigDecimal monthFood =
                foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, monthStart, monthEnd);
        BigDecimal monthTopUp =
                transactionRepository.sumByTypeBetween(
                        TransactionTypeEnum.TOPUP, monthStart, monthEnd);

        long total = machineRepository.count();
        long inUse = machineRepository.countByStatus(MachineStatusEnum.IN_USE);
        long available = machineRepository.countByStatus(MachineStatusEnum.AVAILABLE);

        return DashboardStatsResponse.builder()
                .todayNetRevenue(todayNet)
                .todayFoodRevenue(todayFood)
                .todayCashTopUp(todayTopUp)
                .todayTotalRevenue(todayNet.add(todayFood))
                .monthNetRevenue(monthNet)
                .monthFoodRevenue(monthFood)
                .monthCashTopUp(monthTopUp)
                .monthTotalRevenue(monthNet.add(monthFood))
                .totalMachines(total)
                .machinesInUse(inUse)
                .machinesAvailable(available)
                .activeSessions(inUse)
                .build();
    }
}
