package com.payhub.payment.gateway;

import java.util.UUID;

/**
 * Port over the Stripe API. Kept as an interface so payment logic can be tested
 * without contacting Stripe.
 */
public interface StripeGateway {

    CheckoutSession createCheckoutSession(CheckoutCommand command);

    /**
     * Verifies the webhook signature and parses the payload into a
     * provider-agnostic {@link WebhookEvent}.
     *
     * @throws com.payhub.payment.exception.WebhookSignatureException if the
     *         signature is invalid
     */
    WebhookEvent constructEvent(String payload, String signatureHeader);

    /** Relevant webhook event types, normalised away from Stripe's strings. */
    enum WebhookEventType {
        CHECKOUT_COMPLETED,
        CHECKOUT_EXPIRED,
        UNKNOWN
    }

    /** A verified, parsed webhook event. */
    record WebhookEvent(String eventId, WebhookEventType type, String sessionId, String paymentIntentId) {
    }

    /** Inputs needed to open a Stripe Checkout session. */
    record CheckoutCommand(
            UUID userId,
            UUID subscriptionId,
            long amountCents,
            String currency,
            String productName,
            String customerEmail) {
    }

    /** The resulting Stripe session and the URL to redirect the customer to. */
    record CheckoutSession(String sessionId, String checkoutUrl) {
    }
}
