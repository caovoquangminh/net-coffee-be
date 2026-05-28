package com.netcoffee.billing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DefaultBillingStrategyTest {

    private final DefaultBillingStrategy strategy = new DefaultBillingStrategy();

    @Test
    @DisplayName("60 giây với giá 8000/giờ → 133.33đ")
    void sixtySeconds_returnsCorrectAmount() {
        assertThat(strategy.calcCharge(new BigDecimal("8000"), 60)).isEqualByComparingTo("133.33");
    }

    @Test
    @DisplayName("90 giây (1.5 phút) với giá 8000/giờ → 200.00đ")
    void ninetySeconds_returnsCorrectAmount() {
        assertThat(strategy.calcCharge(new BigDecimal("8000"), 90)).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("1 giây với giá 8000/giờ → 2.22đ")
    void oneSecond_returnsCorrectAmount() {
        assertThat(strategy.calcCharge(new BigDecimal("8000"), 1)).isEqualByComparingTo("2.22");
    }

    @Test
    @DisplayName("0 giây → 0đ")
    void zeroSeconds_returnsZero() {
        assertThat(strategy.calcCharge(new BigDecimal("8000"), 0))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Số giây âm → 0đ")
    void negativeSeconds_returnsZero() {
        assertThat(strategy.calcCharge(new BigDecimal("8000"), -5))
                .isEqualByComparingTo(BigDecimal.ZERO);
    }
}
