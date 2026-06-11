package com.wex.exchange.error;

/** The Treasury service answered, but with an error status or a payload we couldn't trust. */
public class TreasuryBadGatewayException extends RuntimeException {

    public TreasuryBadGatewayException(String message) {
        super(message);
    }

    public TreasuryBadGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
