package com.wex.exchange.service;

import com.wex.exchange.client.TreasuryRatesClient;
import com.wex.exchange.domain.ConvertedPurchase;
import com.wex.exchange.domain.ExchangeRate;
import com.wex.exchange.domain.Purchase;
import com.wex.exchange.error.ConversionUnavailableException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrencyConversionServiceTest {

    private static final UUID ID = UUID.randomUUID();
    private static final LocalDate PURCHASE_DATE = LocalDate.of(2025, 12, 31);
    private static final String CURRENCY = "Canada-Dollar";

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private TreasuryRatesClient ratesClient;

    @InjectMocks
    private CurrencyConversionService conversionService;

    @Test
    void looksUpTheRateOverTheSixMonthsEndingOnThePurchaseDate() {
        givenPurchase(new BigDecimal("20.00"));
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.369"), PURCHASE_DATE)));

        conversionService.convert(ID, CURRENCY);

        verify(ratesClient).findRate(CURRENCY, LocalDate.of(2025, 6, 30), PURCHASE_DATE);
    }

    @Test
    void convertsTheStoredAmountUsingTheReturnedRate() {
        givenPurchase(new BigDecimal("20.00"));
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.369"), PURCHASE_DATE)));

        ConvertedPurchase converted = conversionService.convert(ID, CURRENCY);

        assertThat(converted.convertedAmount()).isEqualByComparingTo("27.38");
        assertThat(converted.convertedAmount().scale()).isEqualTo(2);
        assertThat(converted.currency()).isEqualTo(CURRENCY);
    }

    @Test
    void keepsTheRatesNativePrecisionInTheResult() {
        givenPurchase(new BigDecimal("20.00"));
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.3690"), PURCHASE_DATE)));

        ConvertedPurchase converted = conversionService.convert(ID, CURRENCY);

        assertThat(converted.exchangeRate().toPlainString()).isEqualTo("1.3690");
    }

    @Test
    void failsWhenNoRateIsAvailable() {
        givenPurchase(new BigDecimal("20.00"));
        when(ratesClient.findRate(eq(CURRENCY), any(), any())).thenReturn(Optional.empty());

        assertThatExceptionOfType(ConversionUnavailableException.class)
                .isThrownBy(() -> conversionService.convert(ID, CURRENCY))
                .withMessageContaining(CURRENCY);
    }

    @Test
    void rejectsARateDatedOutsideTheWindow() {
        givenPurchase(new BigDecimal("20.00"));
        // Older than the six-month window — should be treated as if no rate came back at all.
        LocalDate tooOld = LocalDate.of(2025, 1, 1);
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.369"), tooOld)));

        assertThatExceptionOfType(ConversionUnavailableException.class)
                .isThrownBy(() -> conversionService.convert(ID, CURRENCY));
    }

    @Test
    void acceptsARateDatedExactlyOnTheStartOfTheWindow() {
        givenPurchase(new BigDecimal("20.00"));
        // Window starts six calendar months before 2025-12-31, i.e. 2025-06-30 — and is inclusive.
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.369"), LocalDate.of(2025, 6, 30))));

        assertThat(conversionService.convert(ID, CURRENCY).convertedAmount()).isEqualByComparingTo("27.38");
    }

    @Test
    void rejectsARateOneDayBeforeTheWindowStarts() {
        givenPurchase(new BigDecimal("20.00"));
        when(ratesClient.findRate(eq(CURRENCY), any(), any()))
                .thenReturn(Optional.of(rate(new BigDecimal("1.369"), LocalDate.of(2025, 6, 29))));

        assertThatExceptionOfType(ConversionUnavailableException.class)
                .isThrownBy(() -> conversionService.convert(ID, CURRENCY));
    }

    private void givenPurchase(BigDecimal amount) {
        when(purchaseService.getById(ID)).thenReturn(new Purchase(ID, "Office supplies", PURCHASE_DATE, amount));
    }

    private static ExchangeRate rate(BigDecimal value, LocalDate recordDate) {
        return new ExchangeRate(CURRENCY, value, recordDate);
    }
}
