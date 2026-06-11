package com.wex.exchange.error;

import java.time.Instant;
import java.util.Map;

/**
 * The single error shape every failed request returns. {@code fieldErrors} is populated only for
 * request-validation failures and is null otherwise.
 */
public record ApiError(
        int status,
        String error,
        String message,
        Map<String, String> fieldErrors,
        Instant timestamp) {
}
