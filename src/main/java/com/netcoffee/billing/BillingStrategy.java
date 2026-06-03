package com.netcoffee.billing;

import java.math.BigDecimal;

public interface BillingStrategy {
    BigDecimal calcCharge(BigDecimal pricePerHour, long seconds);
}
