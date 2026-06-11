package com.wex.exchange.client;

import com.wex.exchange.domain.ExchangeRate;
import com.wex.exchange.error.TreasuryBadGatewayException;
import com.wex.exchange.error.TreasuryUnavailableException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class TreasuryRatesClientTest {

    private static final LocalDate WINDOW_START = LocalDate.of(2025, 6, 30);
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2025, 12, 31);

    private MockWebServer server;
    private TreasuryRatesClient client;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(1))
                .withReadTimeout(Duration.ofMillis(500));
        RestClient restClient = RestClient.builder()
                .requestFactory(ClientHttpRequestFactories.get(settings))
                .build();
        client = new TreasuryRatesClient(restClient, server.url("/rates").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void asksTheTreasuryForTheSingleMostRecentRateInTheWindow() throws InterruptedException {
        server.enqueue(jsonBody("""
                {"data":[{"country_currency_desc":"Canada-Dollar","exchange_rate":"1.369","record_date":"2025-12-31"}]}
                """));

        client.findRate("Canada-Dollar", WINDOW_START, PURCHASE_DATE);

        String path = server.takeRequest().getPath();
        assertThat(path).contains("fields=country_currency_desc,exchange_rate,record_date");
        assertThat(path).contains(
                "filter=country_currency_desc:eq:Canada-Dollar,record_date:lte:2025-12-31,record_date:gte:2025-06-30");
        assertThat(path).contains("sort=-record_date");
        assertThat(path).contains("page%5Bsize%5D=1");
    }

    @Test
    void percentEncodesCurrencyValuesThatContainSpaces() throws InterruptedException {
        server.enqueue(jsonBody("{\"data\":[]}"));

        client.findRate("Euro Zone-Euro", WINDOW_START, PURCHASE_DATE);

        assertThat(server.takeRequest().getPath()).contains("country_currency_desc:eq:Euro%20Zone-Euro");
    }

    @Test
    void parsesTheRateAndRecordDate() {
        server.enqueue(jsonBody("""
                {"data":[{"country_currency_desc":"Canada-Dollar","exchange_rate":"1.369","record_date":"2025-12-31"}]}
                """));

        Optional<ExchangeRate> rate = client.findRate("Canada-Dollar", WINDOW_START, PURCHASE_DATE);

        assertThat(rate).isPresent();
        assertThat(rate.get().rate()).isEqualByComparingTo("1.369");
        assertThat(rate.get().recordDate()).isEqualTo(PURCHASE_DATE);
    }

    @Test
    void returnsEmptyWhenTheDatasetHasNoMatchingRow() {
        server.enqueue(jsonBody("{\"data\":[]}"));

        assertThat(client.findRate("Atlantis-Doubloon", WINDOW_START, PURCHASE_DATE)).isEmpty();
    }

    @Test
    void mapsAnUpstreamErrorStatusToBadGateway() {
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThatExceptionOfType(TreasuryBadGatewayException.class)
                .isThrownBy(() -> client.findRate("Canada-Dollar", WINDOW_START, PURCHASE_DATE));
    }

    @Test
    void mapsANonNumericRateToBadGateway() {
        server.enqueue(jsonBody("""
                {"data":[{"country_currency_desc":"Canada-Dollar","exchange_rate":"n/a","record_date":"2025-12-31"}]}
                """));

        assertThatExceptionOfType(TreasuryBadGatewayException.class)
                .isThrownBy(() -> client.findRate("Canada-Dollar", WINDOW_START, PURCHASE_DATE));
    }

    @Test
    void mapsAReadTimeoutToServiceUnavailable() {
        server.enqueue(new MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE));

        assertThatExceptionOfType(TreasuryUnavailableException.class)
                .isThrownBy(() -> client.findRate("Canada-Dollar", WINDOW_START, PURCHASE_DATE));
    }

    private static MockResponse jsonBody(String body) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(body);
    }
}
