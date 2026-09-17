package com.payhub.webhook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.webhook.domain.ProcessedWebhookEvent;
import com.payhub.webhook.repository.ProcessedWebhookEventRepository;

/**
 * Guards against processing the same Stripe event twice. Joins the caller's
 * transaction so the "processed" marker commits atomically with the effects of
 * handling the event — if handling fails, the marker is rolled back and Stripe's
 * retry is processed normally.
 */
@Service
public class WebhookIdempotencyService {

    private final ProcessedWebhookEventRepository repository;

    public WebhookIdempotencyService(ProcessedWebhookEventRepository repository) {
        this.repository = repository;
    }

    /**
     * Records the event as processed and returns {@code true} if this is the
     * first time it has been seen; {@code false} if it was already processed.
     * The unique constraint on {@code stripe_event_id} is the backstop for
     * concurrent duplicate deliveries (the losing insert fails and its
     * transaction rolls back, so Stripe simply retries and then sees it as a
     * duplicate).
     */
    @Transactional
    public boolean markIfFirst(String stripeEventId, String eventType) {
        if (repository.existsByStripeEventId(stripeEventId)) {
            return false;
        }
        repository.saveAndFlush(ProcessedWebhookEvent.of(stripeEventId, eventType));
        return true;
    }
}
