package com.wex.exchange.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class MoneyMathTest {

    @Test
    void roundsHalfUpToTheNearestCent() {
        assertThat(MoneyMath.toCents(new BigDecimal("19.995"))).isEqualByComparingTo("20.00");
        assertThat(MoneyMath.toCents(new BigDecimal("19.994"))).isEqualByComparingTo("19.99");
        assertThat(MoneyMath.toCents(new BigDecimal("0.005"))).isEqualByComparingTo("0.01");
    }

    @Test
    void normalisedAmountAlwaysHasTwoDecimalPlaces() {
        assertThat(MoneyMath.toCents(new BigDecimal("5")).scale()).isEqualTo(2);
        assertThat(MoneyMath.toCents(new BigDecimal("5.1")).scale()).isEqualTo(2);
    }

    @Test
    void convertsByMultiplyingThenRoundingOnce() {
        BigDecimal converted = MoneyMath.convert(new BigDecimal("20.00"), new BigDecimal("1.369"));
        assertThat(converted).isEqualByComparingTo("27.38");
        assertThat(converted.scale()).isEqualTo(2);
    }

    @Test
    void roundsOnlyAtTheEndSoCentsDoNotDrift() {
        // 50.00 * 0.6543 = 32.715, which rounds up to 32.72. Rounding the rate first would lose this.
        assertThat(MoneyMath.convert(new BigDecimal("50.00"), new BigDecimal("0.6543")))
                .isEqualByComparingTo("32.72");
    }
}
