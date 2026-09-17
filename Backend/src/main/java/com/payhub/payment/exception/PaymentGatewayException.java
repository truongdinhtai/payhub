package com.payhub.payment.exception;

/** Raised when the payment provider (Stripe) cannot be reached or errors. */
public class PaymentGatewayException extends RuntimeException {

    public PaymentGatewayException(String message, Throwable cause) {
        super(message, cause);
    }
}
