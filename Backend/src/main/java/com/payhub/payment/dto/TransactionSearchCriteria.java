package com.payhub.payment.dto;

import java.time.Instant;

import com.payhub.payment.domain.TransactionStatus;

/**
 * Optional filters for searching transactions. {@code query} is a full-text
 * search over customer name / email / description; the rest are exact/range
 * filters. Any field may be null (meaning "no filter on this field").
 */
public record TransactionSearchCriteria(
        String query,
        TransactionStatus status,
        Instant from,
        Instant to) {
}
