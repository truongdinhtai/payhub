package com.payhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.payhub.support.AbstractIntegrationTest;

/**
 * Smoke test: the full application context boots against a real PostgreSQL
 * container and Flyway migrations apply cleanly.
 */
@SpringBootTest
class PayhubApplicationTests extends AbstractIntegrationTest {

    @Test
    void contextLoads() {
        // Context startup (including Flyway migration + JPA validation) is the assertion.
    }
}
