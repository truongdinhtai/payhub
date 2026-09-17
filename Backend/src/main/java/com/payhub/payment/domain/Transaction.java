package com.payhub.payment.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A payment transaction tied to a Stripe checkout. Created as {@code PENDING}
 * when checkout starts; the outcome ({@code SUCCEEDED}/{@code FAILED}) is
 * applied later by the Stripe webhook. {@code customerName}/{@code customerEmail}
 * are denormalised from the user so the transaction history can be searched
 * without a join.
 */
@Entity
@Table(name = "transactions")
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "subscription_id")
    private UUID subscriptionId;

    @Column(name = "stripe_session_id", unique = true)
    private String stripeSessionId;

    @Column(name = "stripe_payment_intent_id", unique = true)
    private String stripePaymentIntentId;

    @Column(name = "amount_cents", nullable = false)
    private long amountCents;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TransactionStatus status;

    @Column(length = 255)
    private String description;

    @Column(name = "customer_name", length = 255)
    private String customerName;

    @Column(name = "customer_email", length = 320)
    private String customerEmail;

    @Column(name = "target_plan_code", length = 32)
    private String targetPlanCode;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Transaction() {
        // for JPA
    }

    /** Creates a PENDING transaction for a starting checkout. */
    public static Transaction pending(UUID userId, UUID subscriptionId, long amountCents,
                                      String currency, String description,
                                      String customerName, String customerEmail,
                                      String targetPlanCode) {
        Transaction transaction = new Transaction();
        transaction.userId = userId;
        transaction.subscriptionId = subscriptionId;
        transaction.amountCents = amountCents;
        transaction.currency = currency;
        transaction.status = TransactionStatus.PENDING;
        transaction.description = description;
        transaction.customerName = customerName;
        transaction.customerEmail = customerEmail;
        transaction.targetPlanCode = targetPlanCode;
        return transaction;
    }

    public void attachStripeSession(String stripeSessionId) {
        this.stripeSessionId = stripeSessionId;
    }

    /** Marks the transaction succeeded (applied by the webhook). */
    public void markSucceeded(String stripePaymentIntentId) {
        this.status = TransactionStatus.SUCCEEDED;
        this.stripePaymentIntentId = stripePaymentIntentId;
    }

    /** Marks the transaction failed (applied by the webhook). */
    public void markFailed() {
        this.status = TransactionStatus.FAILED;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getSubscriptionId() {
        return subscriptionId;
    }

    public String getStripeSessionId() {
        return stripeSessionId;
    }

    public String getStripePaymentIntentId() {
        return stripePaymentIntentId;
    }

    public long getAmountCents() {
        return amountCents;
    }

    public String getCurrency() {
        return currency;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public String getDescription() {
        return description;
    }

    public String getCustomerName() {
        return customerName;
    }

    public String getCustomerEmail() {
        return customerEmail;
    }

    public String getTargetPlanCode() {
        return targetPlanCode;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
