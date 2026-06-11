package com.wex.exchange.domain;

import com.wex.exchange.money.MoneyMath;

import java.math.BigDecimal;

/**
 * A purchase expressed in a target currency. This is a computed view — it's never stored — so the
 * conversion lives in {@link #from}, the single place the rate is applied to the amount.
 */
public record ConvertedPurchase(
        Purchase purchase,
        String currency,
        BigDecimal exchangeRate,
        BigDecimal convertedAmount) {

    public static ConvertedPurchase from(Purchase purchase, ExchangeRate rate) {
        BigDecimal converted = MoneyMath.convert(purchase.amount(), rate.rate());
        return new ConvertedPurchase(purchase, rate.countryCurrencyDesc(), rate.rate(), converted);
    }
}
