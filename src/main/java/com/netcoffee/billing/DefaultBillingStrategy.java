package com.netcoffee.billing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class DefaultBillingStrategy implements BillingStrategy {

    @Override
    public BigDecimal calcCharge(BigDecimal pricePerHour, long seconds) {
        if (seconds <= 0) return BigDecimal.ZERO;
        BigDecimal pricePerSecond =
                pricePerHour.divide(BigDecimal.valueOf(3600), 10, RoundingMode.HALF_UP);
        return pricePerSecond
                .multiply(BigDecimal.valueOf(seconds))
                .setScale(2, RoundingMode.HALF_UP);
    }
}
