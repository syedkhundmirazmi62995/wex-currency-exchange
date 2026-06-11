package com.wex.exchange.error;

/**
 * A purchase failed a business rule that Bean Validation on the request body can't express on its
 * own (for example, an amount that only becomes zero once it's rounded to the nearest cent). Carries
 * the offending field so the response can report it the same way a bean-validation failure would.
 */
public class PurchaseValidationException extends RuntimeException {

    private final String field;

    public PurchaseValidationException(String field, String message) {
        super(message);
        this.field = field;
    }

    public String getField() {
        return field;
    }
}
