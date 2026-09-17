package com.payhub.payment;

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

import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.CheckoutSession;
import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * Verifies the paid-plan checkout flow: subscribing to a paid plan opens a
 * (mocked) Stripe session, leaves the subscription INCOMPLETE, and records a
 * PENDING transaction visible only to its owner.
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;

    @MockitoBean
    private StripeGateway stripeGateway;

    @BeforeEach
    void stubStripe() {
        when(stripeGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("sess_test", "https://stripe.test/checkout"));
    }

    @Test
    void subscribingToPaidPlan_opensCheckoutAndRecordsPendingTransaction() throws Exception {
        UUID user = newUser("pay-1", "pay1@example.com");

        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"PRO\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.subscription.status").value("INCOMPLETE"))
                .andExpect(jsonPath("$.checkoutUrl").value("https://stripe.test/checkout"));

        mockMvc.perform(asUser(user, get("/api/v1/transactions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.content[0].amountCents").value(2900))
                .andExpect(jsonPath("$.content[0].customerName").value("Name"));
    }

    @Test
    void transactions_areScopedToTheCurrentUser() throws Exception {
        UUID buyer = newUser("pay-buyer", "buyer@example.com");
        UUID other = newUser("pay-other", "other@example.com");

        mockMvc.perform(asUser(buyer, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"PRO\"}"))
                .andExpect(status().isCreated());

        mockMvc.perform(asUser(other, get("/api/v1/transactions")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private UUID newUser(String sub, String email) {
        User user = userService.upsertFromGoogle(sub, email, "Name", null);
        return user.getId();
    }

    private static MockHttpServletRequestBuilder asUser(UUID userId, MockHttpServletRequestBuilder builder) {
        return builder.with(jwt().jwt(b -> b.subject(userId.toString())));
    }
}
