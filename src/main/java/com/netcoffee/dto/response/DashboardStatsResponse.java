package com.netcoffee.dto.response;

import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DashboardStatsResponse {

    private BigDecimal todayNetRevenue;
    private BigDecimal todayFoodRevenue;
    private BigDecimal todayCashTopUp;
    private BigDecimal todayTotalRevenue;

    private BigDecimal monthNetRevenue;
    private BigDecimal monthFoodRevenue;
    private BigDecimal monthCashTopUp;
    private BigDecimal monthTotalRevenue;

    private long totalMachines;
    private long machinesInUse;
    private long machinesAvailable;
    private long activeSessions;
}
