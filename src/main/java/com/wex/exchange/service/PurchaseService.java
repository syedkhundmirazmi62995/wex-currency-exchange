package com.wex.exchange.service;

import com.wex.exchange.domain.Purchase;
import com.wex.exchange.error.PurchaseNotFoundException;
import com.wex.exchange.error.PurchaseValidationException;
import com.wex.exchange.repository.PurchaseRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.UUID;

@Service
public class PurchaseService {

    private final PurchaseRepository repository;
    private final Clock clock;

    public PurchaseService(PurchaseRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    public Purchase store(String description, LocalDate transactionDate, BigDecimal amount) {
        rejectFutureDate(transactionDate);
        Purchase purchase = Purchase.create(description, transactionDate, amount);
        return repository.save(purchase);
    }

    public Purchase getById(UUID id) {
        return repository.findById(id).orElseThrow(() -> new PurchaseNotFoundException(id));
    }

    private void rejectFutureDate(LocalDate transactionDate) {
        if (transactionDate != null && transactionDate.isAfter(LocalDate.now(clock))) {
            throw new PurchaseValidationException("transactionDate", "must not be in the future");
        }
    }
}
