package com.netcoffee.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOwnerResponse {

    private LocalDate date;

    private BigDecimal netRevenue;
    private BigDecimal serviceRevenue;
    private BigDecimal totalRevenue;
    private BigDecimal customerTotalBalance;
    private double machineUtilizationRate;
    private BigDecimal lossDetectionDiff;
    private boolean lossDetectionAlert;

    private CashFlowStats cashFlow;
    private CustomerDebtStats customerDebt;
    private AccountMonitoringStats accountMonitoring;
    private List<StaffStat> staffStats;
    private MachineStats machineStats;
    private LossDetectionStats lossDetection;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CashFlowStats {
        private BigDecimal topUpCash;
        private BigDecimal topUpBank;
        private BigDecimal topUpAdmin;
        private BigDecimal totalTopUp;
        private BigDecimal serviceFoodRevenue;
        private BigDecimal inventoryExpense;
        private BigDecimal estimatedCashIn;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDebtStats {
        private BigDecimal startOfDayBalance;

        private BigDecimal todayTopUps;
        private BigDecimal todayConsumed;
        private BigDecimal currentBalance;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccountMonitoringStats {
        private List<TopAccount> topAccounts;
        private List<AnomalousAccount> anomalousAccounts;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TopAccount {
            private Long id;
            private String phoneNumber;
            private String fullName;
            private BigDecimal balance;
        }

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class AnomalousAccount {
            private Long id;
            private String phoneNumber;
            private String fullName;
            private BigDecimal actualBalance;
            private BigDecimal expectedBalance;
            private BigDecimal diff;
        }
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StaffStat {
        private Long userId;
        private String fullName;
        private String phoneNumber;
        private BigDecimal totalTopUpAmount;
        private long topUpCount;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MachineStats {
        private int total;
        private int inUse;
        private int available;
        private int maintenance;
        private double utilizationRate;
        private long totalSessionSeconds;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LossDetectionStats {
        private BigDecimal allTimeTopUps;
        private BigDecimal allTimeDeductions;
        private BigDecimal allTimeRefunds;
        private BigDecimal expectedTotalBalance;
        private BigDecimal actualTotalBalance;
        private BigDecimal diff;
        private boolean alert;
    }
}
