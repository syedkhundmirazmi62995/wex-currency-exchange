package com.wex.exchange.domain;

import com.wex.exchange.error.PurchaseValidationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;

class PurchaseTest {

    private static final LocalDate A_DATE = LocalDate.of(2025, 11, 15);

    @Test
    void storesADescriptionDateAndAmountAndAssignsAnId() {
        Purchase purchase = Purchase.create("Office supplies", A_DATE, new BigDecimal("12.34"));

        assertThat(purchase.id()).isNotNull();
        assertThat(purchase.description()).isEqualTo("Office supplies");
        assertThat(purchase.transactionDate()).isEqualTo(A_DATE);
        assertThat(purchase.amount()).isEqualByComparingTo("12.34");
    }

    @Test
    void roundsTheAmountToTheNearestCent() {
        Purchase purchase = Purchase.create("Coffee", A_DATE, new BigDecimal("4.567"));

        assertThat(purchase.amount()).isEqualByComparingTo("4.57");
        assertThat(purchase.amount().scale()).isEqualTo(2);
    }

    @Test
    void trimsSurroundingWhitespaceFromTheDescription() {
        Purchase purchase = Purchase.create("  Lunch  ", A_DATE, new BigDecimal("9.00"));

        assertThat(purchase.description()).isEqualTo("Lunch");
    }

    @Test
    void acceptsADescriptionOfExactlyFiftyCharacters() {
        String fifty = "x".repeat(50);

        assertThatNoException()
                .isThrownBy(() -> Purchase.create(fifty, A_DATE, new BigDecimal("1.00")));
    }

    @Test
    void rejectsADescriptionLongerThanFiftyCharacters() {
        String tooLong = "x".repeat(51);

        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create(tooLong, A_DATE, new BigDecimal("1.00")))
                .matches(ex -> ex.getField().equals("description"));
    }

    @Test
    void rejectsABlankDescription() {
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create("   ", A_DATE, new BigDecimal("1.00")))
                .matches(ex -> ex.getField().equals("description"));
    }

    @Test
    void rejectsAmountsThatAreZeroOrNegative() {
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create("x", A_DATE, BigDecimal.ZERO));
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create("x", A_DATE, new BigDecimal("-1.00")));
    }

    @Test
    void rejectsAmountsThatRoundDownToZero() {
        // 0.004 is positive but becomes 0.00 once rounded, so it isn't a real purchase amount.
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create("x", A_DATE, new BigDecimal("0.004")))
                .matches(ex -> ex.getField().equals("amount"));
    }

    @Test
    void rejectsAMissingTransactionDate() {
        assertThatExceptionOfType(PurchaseValidationException.class)
                .isThrownBy(() -> Purchase.create("x", null, new BigDecimal("1.00")))
                .matches(ex -> ex.getField().equals("transactionDate"));
    }
}
