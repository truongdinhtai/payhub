package com.payhub.webhook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.payhub.payment.exception.WebhookSignatureException;
import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.CheckoutSession;
import com.payhub.payment.gateway.StripeGateway.WebhookEvent;
import com.payhub.payment.gateway.StripeGateway.WebhookEventType;
import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.service.UserService;
import com.payhub.webhook.repository.ProcessedWebhookEventRepository;

/**
 * End-to-end Stripe webhook flow: a successful checkout event activates the
 * subscription and marks the transaction succeeded; duplicate delivery is a
 * no-op; and an invalid signature is rejected.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebhookIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private ProcessedWebhookEventRepository processedWebhookEventRepository;

    @MockitoBean
    private StripeGateway stripeGateway;

    @BeforeEach
    void stubCheckout() {
        when(stripeGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("sess_hook", "https://stripe.test/checkout"));
    }

    @Test
    void checkoutCompleted_activatesSubscriptionAndMarksTransactionSucceeded() throws Exception {
        UUID user = subscribeToPaidPlan();

        when(stripeGateway.constructEvent(any(), any()))
                .thenReturn(new WebhookEvent("evt_1", WebhookEventType.CHECKOUT_COMPLETED, "sess_hook", "pi_1"));

        postWebhook().andExpect(status().isOk());

        mockMvc.perform(asUser(user, get("/api/v1/subscriptions/current")))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
        mockMvc.perform(asUser(user, get("/api/v1/transactions")))
                .andExpect(jsonPath("$.content[0].status").value("SUCCEEDED"));
    }

    @Test
    void duplicateEvent_isProcessedOnlyOnce() throws Exception {
        subscribeToPaidPlan();
        when(stripeGateway.constructEvent(any(), any()))
                .thenReturn(new WebhookEvent("evt_dup", WebhookEventType.CHECKOUT_COMPLETED, "sess_hook", "pi_1"));

        postWebhook().andExpect(status().isOk());
        postWebhook().andExpect(status().isOk());

        assertThat(processedWebhookEventRepository.count()).isEqualTo(1);
    }

    @Test
    void invalidSignature_isRejected() throws Exception {
        when(stripeGateway.constructEvent(any(), any()))
                .thenThrow(new WebhookSignatureException("bad", null));

        postWebhook().andExpect(status().isBadRequest());
    }

    private UUID subscribeToPaidPlan() throws Exception {
        UUID user = userService.upsertFromGoogle("hook-" + UUID.randomUUID(),
                UUID.randomUUID() + "@example.com", "Name", null).getId();
        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"PRO\"}"))
                .andExpect(status().isCreated());
        return user;
    }

    private org.springframework.test.web.servlet.ResultActions postWebhook() throws Exception {
        return mockMvc.perform(post("/api/v1/webhooks/stripe")
                .header("Stripe-Signature", "test-signature")
                .contentType("application/json")
                .content("{}"));
    }

    private static MockHttpServletRequestBuilder asUser(UUID userId, MockHttpServletRequestBuilder builder) {
        return builder.with(jwt().jwt(b -> b.subject(userId.toString())));
    }
}
