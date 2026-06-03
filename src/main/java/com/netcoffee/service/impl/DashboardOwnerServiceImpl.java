package com.netcoffee.service.impl;

import com.netcoffee.dto.response.DashboardOwnerResponse;
import com.netcoffee.dto.response.DashboardOwnerResponse.AccountMonitoringStats;
import com.netcoffee.dto.response.DashboardOwnerResponse.AccountMonitoringStats.AnomalousAccount;
import com.netcoffee.dto.response.DashboardOwnerResponse.AccountMonitoringStats.TopAccount;
import com.netcoffee.dto.response.DashboardOwnerResponse.CashFlowStats;
import com.netcoffee.dto.response.DashboardOwnerResponse.CustomerDebtStats;
import com.netcoffee.dto.response.DashboardOwnerResponse.LossDetectionStats;
import com.netcoffee.dto.response.DashboardOwnerResponse.MachineStats;
import com.netcoffee.dto.response.DashboardOwnerResponse.StaffStat;
import com.netcoffee.entity.TMachineEntity;
import com.netcoffee.entity.TUserEntity;
import com.netcoffee.enumtype.InventoryTransactionTypeEnum;
import com.netcoffee.enumtype.MachineStatusEnum;
import com.netcoffee.enumtype.OrderStatusEnum;
import com.netcoffee.enumtype.PaymentMethodEnum;
import com.netcoffee.enumtype.TransactionTypeEnum;
import com.netcoffee.enumtype.UserRoleEnum;
import com.netcoffee.repository.FoodOrderRepository;
import com.netcoffee.repository.InventoryTransactionRepository;
import com.netcoffee.repository.MachineRepository;
import com.netcoffee.repository.SessionRepository;
import com.netcoffee.repository.TransactionRepository;
import com.netcoffee.repository.UserRepository;
import com.netcoffee.service.DashboardOwnerService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardOwnerServiceImpl implements DashboardOwnerService {

    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final MachineRepository machineRepository;

    @Override
    @Transactional(readOnly = true)
    public DashboardOwnerResponse getOwnerStats(LocalDate date) {
        LocalDateTime from = date.atStartOfDay();
        LocalDateTime to = date.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        // ── Revenue ────────────────────────────────────────────────────────────
        BigDecimal netRevenue =
                transactionRepository.sumByTypeBetween(TransactionTypeEnum.DEDUCT, from, to);
        BigDecimal serviceRevenue =
                foodOrderRepository.sumByStatusBetween(OrderStatusEnum.DONE, from, to);
        BigDecimal totalRevenue = netRevenue.add(serviceRevenue);

        // ── Cash Flow ──────────────────────────────────────────────────────────
        BigDecimal topUpCash =
                transactionRepository.sumByTypeAndMethodBetween(
                        TransactionTypeEnum.TOPUP, PaymentMethodEnum.CASH, from, to);
        BigDecimal topUpBank =
                transactionRepository.sumByTypeAndMethodBetween(
                        TransactionTypeEnum.TOPUP, PaymentMethodEnum.QR_BANK, from, to);
        BigDecimal topUpAdmin =
                transactionRepository.sumByTypeAndMethodBetween(
                        TransactionTypeEnum.TOPUP, PaymentMethodEnum.ADMIN, from, to);
        BigDecimal totalTopUp = topUpCash.add(topUpBank).add(topUpAdmin);
        BigDecimal inventoryExpense =
                inventoryTransactionRepository.sumCostByTypeBetween(
                        InventoryTransactionTypeEnum.IMPORT, from, to);
        BigDecimal estimatedCashIn = totalTopUp.add(serviceRevenue).subtract(inventoryExpense);

        CashFlowStats cashFlow =
                CashFlowStats.builder()
                        .topUpCash(topUpCash)
                        .topUpBank(topUpBank)
                        .topUpAdmin(topUpAdmin)
                        .totalTopUp(totalTopUp)
                        .serviceFoodRevenue(serviceRevenue)
                        .inventoryExpense(inventoryExpense)
                        .estimatedCashIn(estimatedCashIn)
                        .build();

        // ── Customer Debt ──────────────────────────────────────────────────────
        BigDecimal currentBalance = userRepository.sumBalanceByRole(UserRoleEnum.CUSTOMER);
        BigDecimal todayConsumed = netRevenue.add(serviceRevenue);
        BigDecimal startOfDayBalance =
                currentBalance.subtract(totalTopUp).add(todayConsumed).max(BigDecimal.ZERO);

        CustomerDebtStats customerDebt =
                CustomerDebtStats.builder()
                        .startOfDayBalance(startOfDayBalance)
                        .todayTopUps(totalTopUp)
                        .todayConsumed(todayConsumed)
                        .currentBalance(currentBalance)
                        .build();

        // ── Account Monitoring ─────────────────────────────────────────────────
        List<TUserEntity> top20 =
                userRepository.findTop20ByRoleOrderByBalanceDesc(UserRoleEnum.CUSTOMER);
        List<TopAccount> topAccounts =
                top20.stream()
                        .map(
                                u ->
                                        TopAccount.builder()
                                                .id(u.getId())
                                                .phoneNumber(u.getPhoneNumber())
                                                .fullName(u.getFullName())
                                                .balance(u.getBalance())
                                                .build())
                        .collect(Collectors.toList());

        List<Object[]> anomalyRows = userRepository.findAnomalousAccounts();
        List<AnomalousAccount> anomalousAccounts = new ArrayList<>();
        for (Object[] row : anomalyRows) {
            BigDecimal actual = new BigDecimal(row[3].toString());
            BigDecimal expected = new BigDecimal(row[4].toString());
            anomalousAccounts.add(
                    AnomalousAccount.builder()
                            .id(((Number) row[0]).longValue())
                            .phoneNumber((String) row[1])
                            .fullName((String) row[2])
                            .actualBalance(actual)
                            .expectedBalance(expected)
                            .diff(actual.subtract(expected))
                            .build());
        }

        AccountMonitoringStats accountMonitoring =
                AccountMonitoringStats.builder()
                        .topAccounts(topAccounts)
                        .anomalousAccounts(anomalousAccounts)
                        .build();

        // ── Staff Stats ────────────────────────────────────────────────────────
        List<Object[]> staffRows = transactionRepository.staffTopUpStatsBetween(from, to);
        List<Long> staffIds =
                staffRows.stream()
                        .map(r -> ((Number) r[0]).longValue())
                        .collect(Collectors.toList());
        Map<Long, TUserEntity> staffById =
                staffIds.isEmpty()
                        ? Map.of()
                        : userRepository.findAllById(staffIds).stream()
                                .collect(Collectors.toMap(TUserEntity::getId, u -> u));

        List<StaffStat> staffStats = new ArrayList<>();
        for (Object[] row : staffRows) {
            Long uid = ((Number) row[0]).longValue();
            TUserEntity u = staffById.get(uid);
            staffStats.add(
                    StaffStat.builder()
                            .userId(uid)
                            .fullName(u != null ? u.getFullName() : "Unknown")
                            .phoneNumber(u != null ? u.getPhoneNumber() : "")
                            .topUpCount(((Number) row[1]).longValue())
                            .totalTopUpAmount(new BigDecimal(row[2].toString()))
                            .build());
        }

        // ── Machine Stats ──────────────────────────────────────────────────────
        List<TMachineEntity> machines = machineRepository.findAll();
        int total = machines.size();
        int inUse =
                (int)
                        machines.stream()
                                .filter(m -> m.getStatus() == MachineStatusEnum.IN_USE)
                                .count();
        int available =
                (int)
                        machines.stream()
                                .filter(m -> m.getStatus() == MachineStatusEnum.AVAILABLE)
                                .count();
        int maintenance =
                (int)
                        machines.stream()
                                .filter(m -> m.getStatus() == MachineStatusEnum.MAINTENANCE)
                                .count();

        double sessionSeconds = sessionRepository.sumSessionSecondsBetween(from, to, now);
        long elapsedSeconds = Math.max(3600, java.time.Duration.between(from, now).getSeconds());
        double availableSeconds = total > 0 ? (double) total * elapsedSeconds : 1;
        double utilizationRate =
                BigDecimal.valueOf(sessionSeconds / availableSeconds)
                        .min(BigDecimal.ONE)
                        .setScale(4, RoundingMode.HALF_UP)
                        .doubleValue();

        MachineStats machineStats =
                MachineStats.builder()
                        .total(total)
                        .inUse(inUse)
                        .available(available)
                        .maintenance(maintenance)
                        .utilizationRate(utilizationRate)
                        .totalSessionSeconds((long) sessionSeconds)
                        .build();

        // ── Loss Detection ─────────────────────────────────────────────────────
        BigDecimal allTopUps = transactionRepository.sumAllByType(TransactionTypeEnum.TOPUP);
        BigDecimal allDeductions = transactionRepository.sumAllByType(TransactionTypeEnum.DEDUCT);
        BigDecimal allRefunds = transactionRepository.sumAllByType(TransactionTypeEnum.REFUND);
        BigDecimal expectedBalance = allTopUps.subtract(allDeductions).add(allRefunds);
        BigDecimal diff = currentBalance.subtract(expectedBalance);
        boolean lossAlert = diff.abs().compareTo(BigDecimal.ONE) > 0;

        LossDetectionStats lossDetection =
                LossDetectionStats.builder()
                        .allTimeTopUps(allTopUps)
                        .allTimeDeductions(allDeductions)
                        .allTimeRefunds(allRefunds)
                        .expectedTotalBalance(expectedBalance)
                        .actualTotalBalance(currentBalance)
                        .diff(diff)
                        .alert(lossAlert)
                        .build();

        return DashboardOwnerResponse.builder()
                .date(date)
                .netRevenue(netRevenue)
                .serviceRevenue(serviceRevenue)
                .totalRevenue(totalRevenue)
                .customerTotalBalance(currentBalance)
                .machineUtilizationRate(utilizationRate)
                .lossDetectionDiff(diff)
                .lossDetectionAlert(lossAlert)
                .cashFlow(cashFlow)
                .customerDebt(customerDebt)
                .accountMonitoring(accountMonitoring)
                .staffStats(staffStats)
                .machineStats(machineStats)
                .lossDetection(lossDetection)
                .build();
    }
}
