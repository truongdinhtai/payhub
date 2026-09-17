package com.payhub.payment.exception;

/** Raised when a Stripe webhook payload fails signature verification. */
public class WebhookSignatureException extends RuntimeException {

    public WebhookSignatureException(String message, Throwable cause) {
        super(message, cause);
    }
}
