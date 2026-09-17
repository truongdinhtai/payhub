package com.payhub.subscription.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.payhub.subscription.domain.Subscription;

/** Builds the audit snapshot (before/after) for a {@link Subscription}. */
final class SubscriptionSnapshot {

    private SubscriptionSnapshot() {
    }

    static Map<String, Object> of(Subscription subscription) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("planCode", subscription.getPlan().getCode().name());
        snapshot.put("status", subscription.getStatus().name());
        snapshot.put("cancelAtPeriodEnd", subscription.isCancelAtPeriodEnd());
        snapshot.put("currentPeriodEnd", subscription.getCurrentPeriodEnd().toString());
        return snapshot;
    }
}
