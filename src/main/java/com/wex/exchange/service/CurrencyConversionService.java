package com.wex.exchange.service;

import com.wex.exchange.client.TreasuryRatesClient;
import com.wex.exchange.domain.ConvertedPurchase;
import com.wex.exchange.domain.Purchase;
import com.wex.exchange.error.ConversionUnavailableException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.UUID;

@Service
public class CurrencyConversionService {

    /** A rate is only usable if it's no older than this, measured back from the purchase date. */
    private static final int RATE_LOOKBACK_MONTHS = 6;

    private final PurchaseService purchaseService;
    private final TreasuryRatesClient ratesClient;

    public CurrencyConversionService(PurchaseService purchaseService, TreasuryRatesClient ratesClient) {
        this.purchaseService = purchaseService;
        this.ratesClient = ratesClient;
    }

    public ConvertedPurchase convert(UUID purchaseId, String currency) {
        Purchase purchase = purchaseService.getById(purchaseId);

        LocalDate purchaseDate = purchase.transactionDate();
        LocalDate windowStart = purchaseDate.minusMonths(RATE_LOOKBACK_MONTHS);

        return ratesClient.findRate(currency, windowStart, purchaseDate)
                .filter(rate -> isWithinWindow(rate.recordDate(), windowStart, purchaseDate))
                .map(rate -> ConvertedPurchase.from(purchase, rate))
                .orElseThrow(() -> new ConversionUnavailableException(currency, purchaseDate));
    }

    private boolean isWithinWindow(LocalDate date, LocalDate start, LocalDate end) {
        return !date.isBefore(start) && !date.isAfter(end);
    }
}
