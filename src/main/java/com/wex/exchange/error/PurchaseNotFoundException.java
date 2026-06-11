package com.wex.exchange.error;

import java.util.UUID;

public class PurchaseNotFoundException extends RuntimeException {

    public PurchaseNotFoundException(UUID id) {
        super("Purchase " + id + " not found");
    }
}
