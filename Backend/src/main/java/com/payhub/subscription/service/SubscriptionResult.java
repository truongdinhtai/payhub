package com.payhub.subscription.service;

import com.payhub.subscription.domain.Subscription;

/**
 * Outcome of a subscribe call. For free plans the subscription is active and
 * {@code checkoutUrl} is null; for paid plans the subscription is INCOMPLETE and
 * {@code checkoutUrl} points at Stripe Checkout.
 */
public record SubscriptionResult(Subscription subscription, String checkoutUrl) {

    public static SubscriptionResult activated(Subscription subscription) {
        return new SubscriptionResult(subscription, null);
    }

    public static SubscriptionResult checkout(Subscription subscription, String checkoutUrl) {
        return new SubscriptionResult(subscription, checkoutUrl);
    }
}
