package com.payhub.audit;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * Verifies that subscription changes leave an accurate audit trail with real
 * before/after JSONB snapshots, and that the trail is tenant-scoped.
 */
@SpringBootTest
@AutoConfigureMockMvc
class AuditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;

    @Test
    void subscriptionChanges_produceOrderedAuditTrailWithBeforeAfter() throws Exception {
        UUID user = newUser("aud-1", "aud1@example.com");

        subscribe(user, "FREE");
        cancel(user);

        // Newest first: CANCELED, then CREATED — with before/after JSONB snapshots.
        mockMvc.perform(asUser(user, get("/api/v1/audit-logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].action").value("SUBSCRIPTION_CANCELED"))
                .andExpect(jsonPath("$[0].before.status").value("ACTIVE"))
                .andExpect(jsonPath("$[0].after.status").value("CANCELED"))
                .andExpect(jsonPath("$[1].action").value("SUBSCRIPTION_CREATED"))
                .andExpect(jsonPath("$[1].before").isEmpty())
                .andExpect(jsonPath("$[1].after.planCode").value("FREE"));
    }

    @Test
    void auditTrail_isScopedToTheCurrentUser() throws Exception {
        UUID userA = newUser("aud-a", "auda@example.com");
        UUID userB = newUser("aud-b", "audb@example.com");
        subscribe(userA, "FREE");

        mockMvc.perform(asUser(userB, get("/api/v1/audit-logs")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    private UUID newUser(String sub, String email) {
        User user = userService.upsertFromGoogle(sub, email, "Name", null);
        return user.getId();
    }

    private void subscribe(UUID user, String planCode) throws Exception {
        mockMvc.perform(asUser(user, post("/api/v1/subscriptions"))
                        .contentType("application/json")
                        .content("{\"planCode\":\"" + planCode + "\"}"))
                .andExpect(status().isCreated());
    }

    private void cancel(UUID user) throws Exception {
        mockMvc.perform(asUser(user, delete("/api/v1/subscriptions/current")))
                .andExpect(status().isOk());
    }

    private static MockHttpServletRequestBuilder asUser(UUID userId, MockHttpServletRequestBuilder builder) {
        return builder.with(jwt().jwt(b -> b.subject(userId.toString())));
    }
}
