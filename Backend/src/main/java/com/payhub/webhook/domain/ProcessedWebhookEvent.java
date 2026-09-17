package com.payhub.webhook.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;

/**
 * A Stripe webhook event that has already been processed. The unique
 * {@code stripeEventId} is what makes webhook handling idempotent.
 */
@Entity
@Table(name = "processed_webhook_event")
public class ProcessedWebhookEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "stripe_event_id", nullable = false, unique = true)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @CreationTimestamp
    @Column(name = "processed_at", nullable = false, updatable = false)
    private Instant processedAt;

    protected ProcessedWebhookEvent() {
        // for JPA
    }

    public static ProcessedWebhookEvent of(String stripeEventId, String eventType) {
        ProcessedWebhookEvent event = new ProcessedWebhookEvent();
        event.stripeEventId = stripeEventId;
        event.eventType = eventType;
        return event;
    }

    public UUID getId() {
        return id;
    }

    public String getStripeEventId() {
        return stripeEventId;
    }

    public String getEventType() {
        return eventType;
    }

    public Instant getProcessedAt() {
        return processedAt;
    }
}
