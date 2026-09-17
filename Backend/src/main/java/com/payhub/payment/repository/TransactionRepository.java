package com.payhub.payment.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.payhub.payment.domain.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    List<Transaction> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Transaction> findByStripeSessionId(String stripeSessionId);

    /**
     * Tenant-scoped transaction search. {@code q} is matched with Postgres
     * full-text search over the generated {@code search_vector}; {@code status},
     * {@code from} and {@code to} are optional filters. Null parameters are cast
     * explicitly so Postgres can infer their type in a native query.
     */
    @Query(value = """
            SELECT * FROM transactions t
            WHERE t.user_id = :userId
              AND (CAST(:q AS text) IS NULL
                   OR t.search_vector @@ websearch_to_tsquery('simple', CAST(:q AS text)))
              AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar))
              AND (CAST(:from AS timestamptz) IS NULL OR t.created_at >= CAST(:from AS timestamptz))
              AND (CAST(:to AS timestamptz) IS NULL OR t.created_at <= CAST(:to AS timestamptz))
            ORDER BY t.created_at DESC
            """,
            countQuery = """
            SELECT count(*) FROM transactions t
            WHERE t.user_id = :userId
              AND (CAST(:q AS text) IS NULL
                   OR t.search_vector @@ websearch_to_tsquery('simple', CAST(:q AS text)))
              AND (CAST(:status AS varchar) IS NULL OR t.status = CAST(:status AS varchar))
              AND (CAST(:from AS timestamptz) IS NULL OR t.created_at >= CAST(:from AS timestamptz))
              AND (CAST(:to AS timestamptz) IS NULL OR t.created_at <= CAST(:to AS timestamptz))
            """,
            nativeQuery = true)
    Page<Transaction> search(@Param("userId") UUID userId,
                             @Param("q") String q,
                             @Param("status") String status,
                             @Param("from") Instant from,
                             @Param("to") Instant to,
                             Pageable pageable);
}
