package com.payhub.subscription.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.domain.Subscription;
import com.payhub.subscription.domain.SubscriptionStatus;

/** API view of a subscription. */
public record SubscriptionResponse(
        UUID id,
        PlanCode planCode,
        String planName,
        SubscriptionStatus status,
        Instant currentPeriodStart,
        Instant currentPeriodEnd,
        boolean cancelAtPeriodEnd,
        Instant canceledAt,
        Instant createdAt) {

    public static SubscriptionResponse from(Subscription subscription) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getPlan().getCode(),
                subscription.getPlan().getName(),
                subscription.getStatus(),
                subscription.getCurrentPeriodStart(),
                subscription.getCurrentPeriodEnd(),
                subscription.isCancelAtPeriodEnd(),
                subscription.getCanceledAt(),
                subscription.getCreatedAt());
    }
}
