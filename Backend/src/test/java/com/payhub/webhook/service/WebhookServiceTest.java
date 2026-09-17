package com.payhub.webhook.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.payment.domain.Transaction;
import com.payhub.payment.gateway.StripeGateway.WebhookEvent;
import com.payhub.payment.gateway.StripeGateway.WebhookEventType;
import com.payhub.payment.service.PaymentService;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.service.SubscriptionService;

@ExtendWith(MockitoExtension.class)
class WebhookServiceTest {

    @Mock
    private WebhookIdempotencyService idempotencyService;
    @Mock
    private PaymentService paymentService;
    @Mock
    private SubscriptionService subscriptionService;
    @Mock
    private Transaction transaction;

    private WebhookService service() {
        return new WebhookService(idempotencyService, paymentService, subscriptionService);
    }

    @Test
    void checkoutCompleted_completesPaymentAndActivatesSubscription() {
        UUID subId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        WebhookEvent event = new WebhookEvent("evt_1", WebhookEventType.CHECKOUT_COMPLETED, "sess_1", "pi_1");
        when(idempotencyService.markIfFirst("evt_1", "CHECKOUT_COMPLETED")).thenReturn(true);
        when(paymentService.completeCheckout("sess_1", "pi_1")).thenReturn(transaction);
        when(transaction.getSubscriptionId()).thenReturn(subId);
        when(transaction.getUserId()).thenReturn(userId);
        when(transaction.getTargetPlanCode()).thenReturn("PRO");

        service().handle(event);

        verify(paymentService).completeCheckout("sess_1", "pi_1");
        verify(subscriptionService).applyPaidPlan(subId, PlanCode.PRO, userId);
    }

    @Test
    void checkoutExpired_failsPayment() {
        WebhookEvent event = new WebhookEvent("evt_2", WebhookEventType.CHECKOUT_EXPIRED, "sess_2", null);
        when(idempotencyService.markIfFirst("evt_2", "CHECKOUT_EXPIRED")).thenReturn(true);

        service().handle(event);

        verify(paymentService).failCheckout("sess_2");
        verify(subscriptionService, never()).applyPaidPlan(any(), any(), any());
    }

    @Test
    void duplicateEvent_isIgnored() {
        WebhookEvent event = new WebhookEvent("evt_1", WebhookEventType.CHECKOUT_COMPLETED, "sess_1", "pi_1");
        when(idempotencyService.markIfFirst(anyString(), anyString())).thenReturn(false);

        service().handle(event);

        verifyNoInteractions(paymentService);
        verifyNoInteractions(subscriptionService);
    }

    @Test
    void unknownEvent_isAcknowledgedWithoutSideEffects() {
        WebhookEvent event = new WebhookEvent("evt_3", WebhookEventType.UNKNOWN, null, null);
        when(idempotencyService.markIfFirst(eq("evt_3"), anyString())).thenReturn(true);

        service().handle(event);

        verifyNoInteractions(paymentService);
        verifyNoInteractions(subscriptionService);
    }
}
