package com.wex.exchange.service;

import com.wex.exchange.domain.Purchase;
import com.wex.exchange.error.PurchaseNotFoundException;
import com.wex.exchange.error.PurchaseValidationException;
import com.wex.exchange.repository.InMemoryPurchaseRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class PurchaseServiceTest {

    private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-01-15T00:00:00Z"), ZoneOffset.UTC);

    private final PurchaseService service = new PurchaseService(new InMemoryPurchaseRepository(), CLOCK);

    @Test
    void storesAPurchaseAndReturnsItWithAnId() {
        Purchase stored = service.store("Lunch", LocalDate.of(2025, 12, 1), new BigDecimal("18.50"));

        assertThat(stored.id()).isNotNull();
        assertThat(service.getById(stored.id())).isEqualTo(stored);
    }

    @Test
    void throwsWhenAPurchaseDoesNotExist() {
        assertThatExceptionOfType(PurchaseNotFoundException.class)
                .isThrownBy(() -> service.getById(UUID.randomUUID()));
    }

    @Test
    void allowsATransactionDatedToday() {
        Purchase stored = service.store("Today", LocalDate.of(2026, 1, 15), new BigDecimal("1.00"));

        assertThat(stored.transactionDate()).isEqualTo(LocalDate.of(2026, 1, 15));
    }

    @Test
    void rejectsAFutureTransactionDate() {
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> service.store("Tomorrow", LocalDate.of(2026, 1, 16), new BigDecimal("1.00")))
                .matches(ex -> ex.getField().equals("transactionDate"));
    }
}
