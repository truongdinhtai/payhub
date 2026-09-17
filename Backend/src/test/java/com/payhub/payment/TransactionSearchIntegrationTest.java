package com.payhub.payment;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import com.payhub.payment.domain.Transaction;
import com.payhub.payment.domain.TransactionStatus;
import com.payhub.payment.repository.TransactionRepository;
import com.payhub.support.AbstractIntegrationTest;
import com.payhub.user.service.UserService;

/**
 * Exercises the Postgres tsvector full-text search and filters against a real
 * database (H2 cannot emulate tsvector).
 */
@SpringBootTest
@AutoConfigureMockMvc
class TransactionSearchIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private UserService userService;
    @Autowired
    private TransactionRepository transactionRepository;

    @Test
    void fullTextSearch_matchesByCustomerName() throws Exception {
        UUID user = newUser("s1", "s1@example.com");
        seed(user, "Alice Nguyen", TransactionStatus.PENDING);
        seed(user, "Alice Le", TransactionStatus.FAILED);
        seed(user, "Bob Tran", TransactionStatus.SUCCEEDED);

        mockMvc.perform(asUser(user, get("/api/v1/transactions").param("q", "alice")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2));

        mockMvc.perform(asUser(user, get("/api/v1/transactions").param("q", "bob")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filters_byStatusAndCombinedWithQuery() throws Exception {
        UUID user = newUser("s2", "s2@example.com");
        seed(user, "Alice Nguyen", TransactionStatus.PENDING);
        seed(user, "Alice Le", TransactionStatus.FAILED);
        seed(user, "Bob Tran", TransactionStatus.SUCCEEDED);

        mockMvc.perform(asUser(user, get("/api/v1/transactions").param("status", "SUCCEEDED")))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(asUser(user, get("/api/v1/transactions")
                        .param("q", "alice").param("status", "FAILED")))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void filters_byCreatedAtRange() throws Exception {
        UUID user = newUser("s3", "s3@example.com");
        seed(user, "Alice Nguyen", TransactionStatus.PENDING);

        String future = Instant.now().plus(1, ChronoUnit.DAYS).toString();
        mockMvc.perform(asUser(user, get("/api/v1/transactions").param("from", future)))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void pagination_reportsTotalsAndLimitsContent() throws Exception {
        UUID user = newUser("s4", "s4@example.com");
        seed(user, "Alice Nguyen", TransactionStatus.PENDING);
        seed(user, "Bob Tran", TransactionStatus.PENDING);
        seed(user, "Carol Pham", TransactionStatus.PENDING);

        mockMvc.perform(asUser(user, get("/api/v1/transactions")
                        .param("page", "0").param("size", "2")))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.last").value(false))
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void search_isScopedToTheCurrentUser() throws Exception {
        UUID userA = newUser("s5a", "s5a@example.com");
        UUID userB = newUser("s5b", "s5b@example.com");
        seed(userA, "Alice Nguyen", TransactionStatus.PENDING);

        mockMvc.perform(asUser(userB, get("/api/v1/transactions").param("q", "alice")))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    private UUID newUser(String sub, String email) {
        return userService.upsertFromGoogle(sub, email, "Name", null).getId();
    }

    private void seed(UUID userId, String customerName, TransactionStatus status) {
        String email = customerName.toLowerCase().replace(' ', '.') + "@example.com";
        Transaction transaction = Transaction.pending(userId, null, 1000L, "USD",
                "monthly charge", customerName, email, "PRO");
        if (status == TransactionStatus.SUCCEEDED) {
            transaction.markSucceeded("pi_" + UUID.randomUUID());
        } else if (status == TransactionStatus.FAILED) {
            transaction.markFailed();
        }
        transactionRepository.save(transaction);
    }

    private static MockHttpServletRequestBuilder asUser(UUID userId, MockHttpServletRequestBuilder builder) {
        return builder.with(jwt().jwt(b -> b.subject(userId.toString())));
    }
}
