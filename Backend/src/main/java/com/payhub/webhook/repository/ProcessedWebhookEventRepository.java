package com.payhub.webhook.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payhub.webhook.domain.ProcessedWebhookEvent;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, UUID> {

    boolean existsByStripeEventId(String stripeEventId);
}
