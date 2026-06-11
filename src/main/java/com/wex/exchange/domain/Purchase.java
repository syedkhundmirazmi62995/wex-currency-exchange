package com.wex.exchange.domain;

import com.wex.exchange.error.PurchaseValidationException;
import com.wex.exchange.money.MoneyMath;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * A stored purchase in U.S. dollars. Instances are created through {@link #create} so an invalid
 * purchase can never exist: the description and amount are validated and the amount is normalized to
 * cents before an id is assigned.
 */
public record Purchase(UUID id, String description, LocalDate transactionDate, BigDecimal amount) {

    public static final int MAX_DESCRIPTION_LENGTH = 50;

    public static Purchase create(String description, LocalDate transactionDate, BigDecimal amount) {
        String cleanedDescription = requireValidDescription(description);
        if (transactionDate == null) {
            throw new PurchaseValidationException("transactionDate", "must not be null");
        }
        BigDecimal cents = requirePositiveAmount(amount);
        return new Purchase(UUID.randomUUID(), cleanedDescription, transactionDate, cents);
    }

    private static String requireValidDescription(String description) {
        if (description == null || description.isBlank()) {
            throw new PurchaseValidationException("description", "must not be blank");
        }
        String trimmed = description.strip();
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new PurchaseValidationException("description",
                    "must not exceed " + MAX_DESCRIPTION_LENGTH + " characters");
        }
        return trimmed;
    }

    private static BigDecimal requirePositiveAmount(BigDecimal amount) {
        if (amount == null) {
            throw new PurchaseValidationException("amount", "must not be null");
        }
        // Round first, then check the sign: an amount like 0.004 is "positive" but rounds to 0.00,
        // which isn't a real purchase, so it's rejected here rather than stored as zero.
        BigDecimal cents = MoneyMath.toCents(amount);
        if (cents.signum() <= 0) {
            throw new PurchaseValidationException("amount", "must be greater than 0");
        }
        return cents;
    }
}
