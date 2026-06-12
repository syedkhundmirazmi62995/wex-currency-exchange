package com.wex.exchange.web.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Incoming purchase to store. The amount is intentionally not constrained to two decimals here —
 * a value like 19.995 is accepted and rounded to the nearest cent rather than rejected. The
 * {@code @Digits} cap just keeps out absurd precision/magnitude.
 */
public record CreatePurchaseRequest(

        @NotBlank
        @Size(max = 50, message = "must not exceed 50 characters")
        String description,

        @NotNull
        LocalDate transactionDate,

        @NotNull
        @Positive
        @Digits(integer = 16, fraction = 6)
        BigDecimal amount) {

    public CreatePurchaseRequest {
        // Trim here so the length check sees the same value we actually store, rather than counting
        // surrounding whitespace against the 50-character limit.
        if (description != null) {
            description = description.strip();
        }
    }
}

