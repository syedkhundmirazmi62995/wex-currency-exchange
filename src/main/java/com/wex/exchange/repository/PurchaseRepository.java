package com.wex.exchange.repository;

import com.wex.exchange.domain.Purchase;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistence boundary for purchases. The application only needs to store and look up by id; keeping
 * this an interface means the in-memory store can be swapped for a real database later without
 * touching the services.
 */
public interface PurchaseRepository {

    Purchase save(Purchase purchase);

    Optional<Purchase> findById(UUID id);
}
