package com.payhub.payment.service;

import java.util.LinkedHashMap;
import java.util.Map;

import com.payhub.payment.domain.Transaction;

/** Builds the audit snapshot (before/after) for a {@link Transaction}. */
final class TransactionSnapshot {

    private TransactionSnapshot() {
    }

    static Map<String, Object> of(Transaction transaction) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("status", transaction.getStatus().name());
        snapshot.put("amountCents", transaction.getAmountCents());
        snapshot.put("currency", transaction.getCurrency());
        return snapshot;
    }
}
