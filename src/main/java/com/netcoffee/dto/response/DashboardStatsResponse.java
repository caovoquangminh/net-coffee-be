package com.netcoffee.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class DashboardStatsResponse {

    private BigDecimal todayNetRevenue;
    private BigDecimal todayFoodRevenue;
    private BigDecimal todayTotalRevenue;

    private BigDecimal monthNetRevenue;
    private BigDecimal monthFoodRevenue;
    private BigDecimal monthTotalRevenue;

    private long totalMachines;
    private long machinesInUse;
    private long machinesAvailable;
    private long activeSessions;
}
