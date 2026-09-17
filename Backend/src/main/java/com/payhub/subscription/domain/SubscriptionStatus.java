package com.payhub.subscription.domain;

import java.util.Set;

/** Lifecycle status of a subscription (mirrors typical Stripe states). */
public enum SubscriptionStatus {

    /** Awaiting first successful payment (used once Stripe is wired in). */
    INCOMPLETE,
    /** Live and paid. */
    ACTIVE,
    /** A payment failed; access may be restricted pending retry. */
    PAST_DUE,
    /** Terminated; no longer live. */
    CANCELED;

    private static final Set<SubscriptionStatus> LIVE = Set.of(INCOMPLETE, ACTIVE, PAST_DUE);

    /** Statuses that count as a user's current, occupying subscription. */
    public static Set<SubscriptionStatus> live() {
        return LIVE;
    }

    public boolean isLive() {
        return LIVE.contains(this);
    }
}
