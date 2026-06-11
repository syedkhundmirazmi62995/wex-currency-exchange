package com.wex.exchange.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wex.exchange.domain.ConvertedPurchase;
import com.wex.exchange.domain.Purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record ConvertedPurchaseResponse(
        UUID id,
        String description,
        LocalDate transactionDate,
        @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal originalAmount,
        String currency,
        @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal exchangeRate,
        @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal convertedAmount) {

    public static ConvertedPurchaseResponse from(ConvertedPurchase converted) {
        Purchase purchase = converted.purchase();
        return new ConvertedPurchaseResponse(
                purchase.id(),
                purchase.description(),
                purchase.transactionDate(),
                purchase.amount(),
                converted.currency(),
                converted.exchangeRate(),
                converted.convertedAmount());
    }
}
