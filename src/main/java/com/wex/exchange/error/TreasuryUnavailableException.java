package com.wex.exchange.error;

/** We couldn't reach the Treasury service at all — connection refused, timed out, DNS, etc. */
public class TreasuryUnavailableException extends RuntimeException {

    public TreasuryUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
