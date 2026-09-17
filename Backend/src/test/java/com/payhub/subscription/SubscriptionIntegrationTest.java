package com.payhub.subscription;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.CheckoutSession;
import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * End-to-end subscription flow against a real database, exercising the state
 * machine, the "one live subscription" rule, and tenant isolation.
 */
@SpringBootTest
@AutoConfigureMockMvc
class SubscriptionIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StripeGateway stripeGateway;

    @BeforeEach
    void stubStripe() {
        when(stripeGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("sess_it", "https://stripe.test/checkout"));
    }

    @Test
    void fullLifecycle_subscribeFreeChangeCancel() throws Exception {
        UUID user = newUser("sub-a", "a@example.com");

        // Free plan activates immediately, no checkout URL.
        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"FREE\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.planCode").value("FREE"))
                .andExpect(jsonPath("$.subscription.status").value("ACTIVE"))
                .andExpect(jsonPath("$.checkoutUrl").isEmpty());

        mockMvc.perform(asUser(user, get("/api/v1/subscriptions/current")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.planCode").value("FREE"));

        // Upgrading FREE -> ENTERPRISE is a paid upgrade: returns a checkout URL,
        // and the plan is only applied by the webhook (still FREE for now).
        mockMvc.perform(asUser(user, put("/api/v1/subscriptions/current/plan"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"ENTERPRISE\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.test/checkout"))
                .andExpect(jsonPath("$.subscription.planCode").value("FREE"));

        mockMvc.perform(asUser(user, delete("/api/v1/subscriptions/current")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELED"))
                .andExpect(jsonPath("$.canceledAt").isNotEmpty());

        // After an immediate cancel there is no live subscription, so changing
        // the plan is rejected (404).
        mockMvc.perform(asUser(user, put("/api/v1/subscriptions/current/plan"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"PRO\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void subscribingTwice_isRejected() throws Exception {
        UUID user = newUser("sub-b", "b@example.com");
        subscribe(user, "FREE");

        // Rejected by the "one live subscription" rule before any payment logic.
        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"ENTERPRISE\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidPlanCode_returns400() throws Exception {
        UUID user = newUser("sub-c", "c@example.com");

        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"PLATINUM\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void otherUsersSubscription_isForbidden() throws Exception {
        UUID owner = newUser("sub-owner", "owner@example.com");
        UUID intruder = newUser("sub-intruder", "intruder@example.com");

        String body = subscribe(owner, "FREE");
        String subscriptionId = objectMapper.readTree(body).get("subscription").get("id").asText();

        // Owner can read it...
        mockMvc.perform(asUser(owner, get("/api/v1/subscriptions/" + subscriptionId)))
                .andExpect(status().isOk());

        // ...another user cannot.
        mockMvc.perform(asUser(intruder, get("/api/v1/subscriptions/" + subscriptionId)))
                .andExpect(status().isForbidden());
    }

    private UUID newUser(String sub, String email) {
        User user = userService.upsertFromGoogle(sub, email, "Name", null);
        return user.getId();
    }

    private String subscribe(UUID user, String planCode) throws Exception {
        return mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"" + planCode + "\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
    }

    private static MockHttpServletRequestBuilder asUser(UUID userId, MockHttpServletRequestBuilder builder) {
        return builder.with(jwt().jwt(b -> b.subject(userId.toString())));
    }
}
