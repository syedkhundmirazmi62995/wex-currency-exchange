package com.wex.exchange.error;

import java.time.LocalDate;

/**
 * No exchange rate could be applied to a purchase: either the currency isn't one the Treasury
 * publishes, or it has no rate on or before the purchase date within the prior six months. The
 * Treasury API returns an empty result set for both, so we surface a single message either way.
 */
public class ConversionUnavailableException extends RuntimeException {

    public ConversionUnavailableException(String currency, LocalDate purchaseDate) {
        super("The purchase cannot be converted to the target currency '" + currency
                + "': no exchange rate on or before " + purchaseDate + " within the prior 6 months.");
    }
}
