package com.wex.exchange.client;

import com.wex.exchange.domain.ExchangeRate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Hits the real Treasury endpoint to confirm the live contract still matches what the rest of the
 * suite stubs. Skipped unless run with {@code -Dlive.tests=true}, so the normal build stays offline.
 */
@EnabledIfSystemProperty(named = "live.tests", matches = "true")
class TreasuryRatesLiveTest {

    private static final String RATES_URL =
            "https://api.fiscaldata.treasury.gov/services/api/fiscal_service/v1/accounting/od/rates_of_exchange";

    @Test
    void fetchesARealRateForCanadaDollar() {
        TreasuryRatesClient client = new TreasuryRatesClient(RestClient.builder().build(), RATES_URL);

        LocalDate purchaseDate = LocalDate.of(2026, 3, 31);
        Optional<ExchangeRate> rate = client.findRate("Canada-Dollar", purchaseDate.minusMonths(6), purchaseDate);

        assertThat(rate).isPresent();
        assertThat(rate.get().rate()).isPositive();
        assertThat(rate.get().recordDate()).isBetween(purchaseDate.minusMonths(6), purchaseDate);
    }
}
