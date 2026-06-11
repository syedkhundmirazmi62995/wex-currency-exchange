package com.wex.exchange;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises the whole stack — controller, services, real RestClient and JSON handling — against a
 * stubbed Treasury endpoint, so nothing here touches the public internet.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PurchaseConversionIntegrationTest {

    private static MockWebServer treasury;

    @Autowired
    private TestRestTemplate rest;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeAll
    static void startTreasury() throws IOException {
        treasury = new MockWebServer();
        treasury.start();
    }

    @AfterAll
    static void stopTreasury() throws IOException {
        treasury.shutdown();
    }

    @DynamicPropertySource
    static void treasuryUrl(DynamicPropertyRegistry registry) {
        registry.add("treasury.rates-url", () -> treasury.url("/rates").toString());
    }

    @Test
    void storesAPurchaseAndReadsItBack() throws Exception {
        UUID id = storePurchase("Office supplies", "2025-12-31", "20.00");

        ResponseEntity<String> response = rest.getForEntity("/api/v1/purchases/" + id, String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("amount").asText()).isEqualTo("20.00");
    }

    @Test
    void convertsAStoredPurchaseUsingTheTreasuryRate() throws Exception {
        UUID id = storePurchase("Office supplies", "2025-12-31", "20.00");
        treasury.enqueue(jsonResponse("""
                {"data":[{"country_currency_desc":"Canada-Dollar","exchange_rate":"1.369","record_date":"2025-12-31"}]}
                """));

        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/purchases/" + id + "/converted?currency=Canada-Dollar", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("originalAmount").asText()).isEqualTo("20.00");
        assertThat(body.get("exchangeRate").asText()).isEqualTo("1.369");
        assertThat(body.get("convertedAmount").asText()).isEqualTo("27.38");
    }

    @Test
    void convertsACurrencyWhoseNameContainsSpaces() throws Exception {
        UUID id = storePurchase("Travel", "2025-12-31", "100.00");
        treasury.enqueue(jsonResponse("""
                {"data":[{"country_currency_desc":"Euro Zone-Euro","exchange_rate":"0.87","record_date":"2025-12-31"}]}
                """));

        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/purchases/" + id + "/converted?currency=Euro Zone-Euro", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        JsonNode body = objectMapper.readTree(response.getBody());
        assertThat(body.get("convertedAmount").asText()).isEqualTo("87.00");
    }

    @Test
    void returns422WhenTheTreasuryHasNoRate() throws Exception {
        UUID id = storePurchase("Office supplies", "2025-12-31", "20.00");
        treasury.enqueue(jsonResponse("{\"data\":[]}"));

        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/purchases/" + id + "/converted?currency=Atlantis-Doubloon", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void returns404WhenConvertingAPurchaseThatDoesNotExist() {
        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/purchases/" + UUID.randomUUID() + "/converted?currency=Canada-Dollar", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void returns502WhenTheTreasuryFails() throws Exception {
        UUID id = storePurchase("Office supplies", "2025-12-31", "20.00");
        treasury.enqueue(new MockResponse().setResponseCode(500));

        ResponseEntity<String> response = rest.getForEntity(
                "/api/v1/purchases/" + id + "/converted?currency=Canada-Dollar", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
    }

    private UUID storePurchase(String description, String date, String amount) throws Exception {
        String body = """
                {"description":"%s","transactionDate":"%s","amount":"%s"}
                """.formatted(description, date, amount);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<String> response = rest.postForEntity(
                "/api/v1/purchases", new HttpEntity<>(body, headers), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString(objectMapper.readTree(response.getBody()).get("id").asText());
    }

    private static MockResponse jsonResponse(String body) {
        return new MockResponse().setHeader("Content-Type", "application/json").setBody(body);
    }
}
