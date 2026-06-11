package com.wex.exchange.web;

import com.wex.exchange.config.ClockConfig;
import com.wex.exchange.domain.ConvertedPurchase;
import com.wex.exchange.domain.Purchase;
import com.wex.exchange.error.ConversionUnavailableException;
import com.wex.exchange.error.PurchaseNotFoundException;
import com.wex.exchange.error.PurchaseValidationException;
import com.wex.exchange.error.TreasuryBadGatewayException;
import com.wex.exchange.error.TreasuryUnavailableException;
import com.wex.exchange.service.CurrencyConversionService;
import com.wex.exchange.service.PurchaseService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PurchaseController.class)
@Import(ClockConfig.class)
class PurchaseControllerTest {

    private static final UUID ID = UUID.fromString("8f2a6c1e-3b4d-4e5f-9a01-2c3d4e5f6a7b");
    private static final LocalDate DATE = LocalDate.of(2025, 12, 31);

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PurchaseService purchaseService;

    @MockBean
    private CurrencyConversionService conversionService;

    @Test
    void storesAPurchaseAndReturns201WithLocation() throws Exception {
        when(purchaseService.store(any(), any(), any())).thenReturn(storedPurchase());

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Office supplies","transactionDate":"2025-12-31","amount":"20.00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "http://localhost/api/v1/purchases/" + ID))
                .andExpect(jsonPath("$.id").value(ID.toString()))
                .andExpect(jsonPath("$.amount").value("20.00"));
    }

    @Test
    void roundsAnAmountWithMoreThanTwoDecimalsInsteadOfRejectingIt() throws Exception {
        when(purchaseService.store(any(), any(), any())).thenReturn(storedPurchase());

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Office supplies","transactionDate":"2025-12-31","amount":"19.995"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.amount").value("20.00"));
    }

    @Test
    void rejectsADescriptionLongerThanFiftyCharacters() throws Exception {
        String body = """
                {"description":"%s","transactionDate":"2025-12-31","amount":"10.00"}
                """.formatted("x".repeat(51));

        mockMvc.perform(post("/api/v1/purchases").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.description").exists());
    }

    @Test
    void rejectsANonPositiveAmount() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Office supplies","transactionDate":"2025-12-31","amount":"-5.00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.amount").exists());
    }

    @Test
    void rejectsAMissingTransactionDate() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Office supplies","amount":"10.00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transactionDate").exists());
    }

    @Test
    void reportsAFutureTransactionDateAsAFieldError() throws Exception {
        when(purchaseService.store(any(), any(), any()))
                .thenThrow(new PurchaseValidationException("transactionDate", "must not be in the future"));

        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"description":"Office supplies","transactionDate":"2999-01-01","amount":"10.00"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.transactionDate").value("must not be in the future"));
    }

    @Test
    void rejectsAMalformedRequestBody() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ not json "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"));
    }

    @Test
    void rejectsAnUnsupportedContentType() throws Exception {
        mockMvc.perform(post("/api/v1/purchases")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("description=Office supplies"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void returns404WhenAPurchaseIsNotFound() throws Exception {
        when(purchaseService.getById(ID)).thenThrow(new PurchaseNotFoundException(ID));

        mockMvc.perform(get("/api/v1/purchases/{id}", ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Purchase " + ID + " not found"));
    }

    @Test
    void rejectsAMalformedPurchaseId() throws Exception {
        mockMvc.perform(get("/api/v1/purchases/{id}", "not-a-uuid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid value for 'id'"));
    }

    @Test
    void requiresTheCurrencyParameterOnConvert() throws Exception {
        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Required query parameter 'currency' is missing"));
    }

    @Test
    void rejectsABlankCurrencyParameter() throws Exception {
        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID).param("currency", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void returnsTheConvertedPurchaseWithMonetaryValuesAsStrings() throws Exception {
        when(conversionService.convert(ID, "Canada-Dollar")).thenReturn(new ConvertedPurchase(
                storedPurchase(), "Canada-Dollar", new BigDecimal("1.369"), new BigDecimal("27.38")));

        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID).param("currency", "Canada-Dollar"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.originalAmount").value("20.00"))
                .andExpect(jsonPath("$.exchangeRate").value("1.369"))
                .andExpect(jsonPath("$.convertedAmount").value("27.38"))
                .andExpect(jsonPath("$.currency").value("Canada-Dollar"));
    }

    @Test
    void returns422WhenTheConversionIsUnavailable() throws Exception {
        when(conversionService.convert(ID, "Atlantis-Doubloon"))
                .thenThrow(new ConversionUnavailableException("Atlantis-Doubloon", DATE));

        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID).param("currency", "Atlantis-Doubloon"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void returns502WhenTheTreasuryReturnsAnError() throws Exception {
        when(conversionService.convert(ID, "Canada-Dollar"))
                .thenThrow(new TreasuryBadGatewayException("upstream 500"));

        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID).param("currency", "Canada-Dollar"))
                .andExpect(status().isBadGateway());
    }

    @Test
    void returns503WhenTheTreasuryIsUnreachable() throws Exception {
        when(conversionService.convert(ID, "Canada-Dollar"))
                .thenThrow(new TreasuryUnavailableException("timeout", new RuntimeException()));

        mockMvc.perform(get("/api/v1/purchases/{id}/converted", ID).param("currency", "Canada-Dollar"))
                .andExpect(status().isServiceUnavailable());
    }

    private static Purchase storedPurchase() {
        return new Purchase(ID, "Office supplies", DATE, new BigDecimal("20.00"));
    }
}
