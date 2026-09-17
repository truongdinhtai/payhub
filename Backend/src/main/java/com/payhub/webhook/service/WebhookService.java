package com.payhub.webhook.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.payment.domain.Transaction;
import com.payhub.payment.gateway.StripeGateway.WebhookEvent;
import com.payhub.payment.service.PaymentService;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.service.SubscriptionService;

/**
 * Orchestrates the effects of a verified Stripe webhook across the payment and
 * subscription modules. Lives above both (rather than inside either) to keep the
 * dependency graph acyclic. The whole method is one transaction, so the
 * idempotency marker and the resulting changes commit — or roll back — together.
 */
@Service
public class WebhookService {

    private final WebhookIdempotencyService idempotencyService;
    private final PaymentService paymentService;
    private final SubscriptionService subscriptionService;

    public WebhookService(WebhookIdempotencyService idempotencyService,
                          PaymentService paymentService,
                          SubscriptionService subscriptionService) {
        this.idempotencyService = idempotencyService;
        this.paymentService = paymentService;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public void handle(WebhookEvent event) {
        if (!idempotencyService.markIfFirst(event.eventId(), event.type().name())) {
            return; // already processed — ignore the duplicate delivery
        }

        switch (event.type()) {
            case CHECKOUT_COMPLETED -> {
                Transaction transaction =
                        paymentService.completeCheckout(event.sessionId(), event.paymentIntentId());
                subscriptionService.applyPaidPlan(
                        transaction.getSubscriptionId(),
                        PlanCode.valueOf(transaction.getTargetPlanCode()),
                        transaction.getUserId());
            }
            case CHECKOUT_EXPIRED -> paymentService.failCheckout(event.sessionId());
            case UNKNOWN -> {
                // Acknowledged and recorded, but no action to take.
            }
        }
    }
}
