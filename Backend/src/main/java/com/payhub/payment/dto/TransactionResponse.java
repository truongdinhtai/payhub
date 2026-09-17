package com.payhub.payment.dto;

import java.time.Instant;
import java.util.UUID;

import com.payhub.payment.domain.Transaction;
import com.payhub.payment.domain.TransactionStatus;

/** API view of a payment transaction. */
public record TransactionResponse(
        UUID id,
        UUID subscriptionId,
        long amountCents,
        String currency,
        TransactionStatus status,
        String description,
        String customerName,
        Instant createdAt) {

    public static TransactionResponse from(Transaction transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getSubscriptionId(),
                transaction.getAmountCents(),
                transaction.getCurrency(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getCustomerName(),
                transaction.getCreatedAt());
    }
}
