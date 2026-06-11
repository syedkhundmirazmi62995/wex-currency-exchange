package com.wex.exchange.client;

import com.wex.exchange.domain.ExchangeRate;
import com.wex.exchange.error.TreasuryBadGatewayException;
import com.wex.exchange.error.TreasuryUnavailableException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriUtils;

import java.math.BigDecimal;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

/**
 * Reads exchange rates from the U.S. Treasury "Reporting Rates of Exchange" dataset.
 *
 * <p>The query asks the API to do the work: filter to the currency and a date window, sort newest
 * first, and return a single row. That way the most recent rate on or before the purchase date comes
 * back as {@code data[0]} and we never page through the full history.
 */
@Component
public class TreasuryRatesClient {

    private static final String FIELDS = "country_currency_desc,exchange_rate,record_date";

    private final RestClient restClient;
    private final String ratesUrl;

    public TreasuryRatesClient(RestClient treasuryRestClient,
                               @Value("${treasury.rates-url}") String ratesUrl) {
        this.restClient = treasuryRestClient;
        this.ratesUrl = ratesUrl;
    }

    /**
     * Finds the most recent published rate for {@code currency} dated between {@code from} and
     * {@code to} (both inclusive). Returns empty when the dataset has no such rate — which also
     * covers a currency the Treasury doesn't publish, since that comes back as an empty result set.
     */
    public Optional<ExchangeRate> findRate(String currency, LocalDate from, LocalDate to) {
        URI uri = buildUri(currency, from, to);

        RatesResponse response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, treasuryResponse) -> {
                        throw new TreasuryBadGatewayException(
                                "Treasury responded with status " + treasuryResponse.getStatusCode());
                    })
                    .body(RatesResponse.class);
        } catch (ResourceAccessException e) {
            throw new TreasuryUnavailableException("Could not reach the Treasury rates service", e);
        }

        List<RateRecord> rows = response == null ? null : response.data();
        if (rows == null || rows.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(toExchangeRate(rows.get(0)));
    }

    private URI buildUri(String currency, LocalDate from, LocalDate to) {
        // Only the currency value is encoded (it can contain spaces and parentheses). The colons and
        // commas that structure the filter, and the brackets in page[size], are left intact because
        // that's the literal syntax the Treasury API expects.
        String encodedCurrency = UriUtils.encode(currency, StandardCharsets.UTF_8);
        String query = "fields=" + FIELDS
                + "&filter=country_currency_desc:eq:" + encodedCurrency
                + ",record_date:lte:" + to
                + ",record_date:gte:" + from
                + "&sort=-record_date"
                + "&page%5Bsize%5D=1";
        return URI.create(ratesUrl + "?" + query);
    }

    private ExchangeRate toExchangeRate(RateRecord row) {
        return new ExchangeRate(row.countryCurrencyDesc(), parseRate(row.exchangeRate()),
                parseRecordDate(row.recordDate()));
    }

    private BigDecimal parseRate(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new TreasuryBadGatewayException("Treasury returned a rate with no value");
        }
        try {
            return new BigDecimal(raw.trim());
        } catch (NumberFormatException e) {
            throw new TreasuryBadGatewayException("Treasury returned a non-numeric rate: " + raw, e);
        }
    }

    private LocalDate parseRecordDate(String raw) {
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new TreasuryBadGatewayException("Treasury returned an unparseable record date: " + raw, e);
        }
    }
}
