package com.payhub.payment.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.payhub.payment.domain.Transaction;
import com.payhub.payment.dto.TransactionSearchCriteria;
import com.payhub.payment.repository.TransactionRepository;

/**
 * Full-text search over a user's transactions. Normalises the criteria (blank
 * query and status become null "no filter") and delegates to the tsvector-backed
 * repository query. Always scoped to the calling user.
 */
@Service
public class TransactionSearchService {

    private final TransactionRepository transactionRepository;

    public TransactionSearchService(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public Page<Transaction> search(UUID userId, TransactionSearchCriteria criteria, Pageable pageable) {
        String query = StringUtils.hasText(criteria.query()) ? criteria.query().trim() : null;
        String status = criteria.status() == null ? null : criteria.status().name();
        return transactionRepository.search(userId, query, status,
                criteria.from(), criteria.to(), pageable);
    }
}
