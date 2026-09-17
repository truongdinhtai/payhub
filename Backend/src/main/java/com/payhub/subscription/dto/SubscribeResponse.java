package com.payhub.subscription.dto;

import com.payhub.subscription.service.SubscriptionResult;

/**
 * Response to a subscribe request. For paid plans {@code checkoutUrl} is set and
 * the caller should redirect to Stripe; for free plans it is null and the
 * subscription is already active.
 */
public record SubscribeResponse(SubscriptionResponse subscription, String checkoutUrl) {

    public static SubscribeResponse from(SubscriptionResult result) {
        return new SubscribeResponse(
                SubscriptionResponse.from(result.subscription()),
                result.checkoutUrl());
    }
}
