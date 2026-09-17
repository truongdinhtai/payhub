package com.payhub.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class for integration tests that need a real database. Uses a
 * PostgreSQL Testcontainer (not H2) because the platform relies on
 * Postgres-specific features such as tsvector full-text search, which H2
 * cannot emulate.
 *
 * <p>The container follows the <b>singleton pattern</b>: it is started once per
 * JVM in a static initializer and never explicitly stopped (Ryuk reaps it on
 * exit). This is deliberate — letting {@code @Testcontainers} stop/restart the
 * container per test class would leave Spring's cached application contexts
 * pointing at a dead container port. {@link ServiceConnection} wires the running
 * container into every context's datasource.
 *
 * <p>Because that single container is shared by every integration test, each
 * test starts from a clean slate: all application tables are truncated before
 * each test (the {@code plans} reference data seeded by Flyway is preserved).
 */
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void resetDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE processed_webhook_event, transactions, "
                + "subscriptions, audit_log, users RESTART IDENTITY CASCADE");
    }
}
