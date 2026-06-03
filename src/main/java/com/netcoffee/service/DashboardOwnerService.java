package com.netcoffee.service;

import com.netcoffee.dto.response.DashboardOwnerResponse;
import java.time.LocalDate;

public interface DashboardOwnerService {
    DashboardOwnerResponse getOwnerStats(LocalDate date);
}
