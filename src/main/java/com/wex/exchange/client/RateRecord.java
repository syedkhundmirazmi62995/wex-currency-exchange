package com.wex.exchange.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * One row from the Treasury rates_of_exchange dataset. The numeric and date fields arrive as strings
 * and are kept as strings here; the client parses them so it can reject a malformed payload cleanly
 * instead of failing during Jackson binding.
 */
public record RateRecord(
        @JsonProperty("country_currency_desc") String countryCurrencyDesc,
        @JsonProperty("exchange_rate") String exchangeRate,
        @JsonProperty("record_date") String recordDate) {
}
