package com.payhub.payment.domain;

/** Lifecycle of a payment transaction. */
public enum TransactionStatus {
    /** Checkout started, awaiting the outcome from Stripe. */
    PENDING,
    SUCCEEDED,
    FAILED,
    REFUNDED
}
