package com.wex.exchange.web.dto;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.wex.exchange.domain.Purchase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record PurchaseResponse(
        UUID id,
        String description,
        LocalDate transactionDate,
        @JsonSerialize(using = MoneyStringSerializer.class) BigDecimal amount) {

    public static PurchaseResponse from(Purchase purchase) {
        return new PurchaseResponse(
                purchase.id(),
                purchase.description(),
                purchase.transactionDate(),
                purchase.amount());
    }
}
