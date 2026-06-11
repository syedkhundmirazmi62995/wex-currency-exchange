package com.wex.exchange.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A single published exchange rate for a currency, as returned by the Treasury. The rate keeps the
 * precision the Treasury gave us; rounding only happens when we apply it to an amount.
 */
public record ExchangeRate(String countryCurrencyDesc, BigDecimal rate, LocalDate recordDate) {
}
