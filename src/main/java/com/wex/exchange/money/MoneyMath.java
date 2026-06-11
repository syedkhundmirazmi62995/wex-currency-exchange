package com.wex.exchange.money;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Central rules for monetary arithmetic. Keeping the scale and rounding mode in one place means a
 * change to the rounding policy (say, switching to banker's rounding) is a single edit rather than a
 * hunt through the codebase.
 */
public final class MoneyMath {

    /** Cents. */
    public static final int SCALE = 2;

    /** "Nearest cent" — round half away from zero, the usual convention for retail amounts. */
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    private MoneyMath() {
    }

    /** Brings an amount to cent precision without losing the original value's sign. */
    public static BigDecimal toCents(BigDecimal amount) {
        return amount.setScale(SCALE, ROUNDING);
    }

    /**
     * Multiplies at full precision and rounds once, at the end. Rounding the rate first or rounding
     * mid-calculation would drift the last cent on larger amounts.
     */
    public static BigDecimal convert(BigDecimal amount, BigDecimal rate) {
        return amount.multiply(rate).setScale(SCALE, ROUNDING);
    }
}
