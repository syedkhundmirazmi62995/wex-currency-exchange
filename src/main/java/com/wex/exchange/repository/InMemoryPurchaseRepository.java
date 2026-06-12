package com.wex.exchange.repository;

import com.wex.exchange.domain.Purchase;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds purchases in a {@link ConcurrentHashMap}. Purchases are immutable, so a stored reference is
 * safe to hand out without copying. Data does not survive a restart, which is fine for this service
 * and is called out in the README.
 */
@Repository
public class InMemoryPurchaseRepository implements PurchaseRepository {

    private final Map<UUID, Purchase> store = new ConcurrentHashMap<>();

    @Override
    public Purchase save(Purchase purchase) {
        Purchase existing = store.putIfAbsent(purchase.id(), purchase);
        if (existing != null) {
            // Two random UUIDs colliding is effectively impossible; if it ever happens we'd rather
            // fail loudly than silently overwrite an unrelated purchase.
            throw new IllegalStateException("Purchase id already in use: " + purchase.id());
        }
        return purchase;
    }

    @Override
    public Optional<Purchase> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }
}
