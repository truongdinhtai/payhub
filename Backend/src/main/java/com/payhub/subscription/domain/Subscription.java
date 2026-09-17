package com.payhub.subscription.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * A user's subscription to a {@link Plan}. Owns its own lifecycle via domain
 * methods rather than exposing setters, so invalid state transitions stay out
 * of reach. The plan is fetched eagerly because it is small reference data that
 * is always needed when a subscription is read.
 */
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "plan_code", nullable = false)
    private Plan plan;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private SubscriptionStatus status;

    @Column(name = "current_period_start", nullable = false)
    private Instant currentPeriodStart;

    @Column(name = "current_period_end", nullable = false)
    private Instant currentPeriodEnd;

    @Column(name = "cancel_at_period_end", nullable = false)
    private boolean cancelAtPeriodEnd;

    @Column(name = "canceled_at")
    private Instant canceledAt;

    @Column(name = "stripe_subscription_id", unique = true)
    private String stripeSubscriptionId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Subscription() {
        // for JPA
    }

    /** Starts a new, active subscription for the given billing period. */
    public static Subscription start(UUID userId, Plan plan, Instant periodStart, Instant periodEnd) {
        return create(userId, plan, periodStart, periodEnd, SubscriptionStatus.ACTIVE);
    }

    /**
     * Starts a subscription awaiting first payment (paid plans). It becomes
     * ACTIVE once the Stripe webhook confirms the payment.
     */
    public static Subscription startIncomplete(UUID userId, Plan plan, Instant periodStart, Instant periodEnd) {
        return create(userId, plan, periodStart, periodEnd, SubscriptionStatus.INCOMPLETE);
    }

    /** Activates an incomplete subscription after payment (applied by the webhook). */
    public void activate() {
        this.status = SubscriptionStatus.ACTIVE;
    }

    private static Subscription create(UUID userId, Plan plan, Instant periodStart,
                                       Instant periodEnd, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.userId = userId;
        subscription.plan = plan;
        subscription.status = status;
        subscription.currentPeriodStart = periodStart;
        subscription.currentPeriodEnd = periodEnd;
        subscription.cancelAtPeriodEnd = false;
        return subscription;
    }

    /** Switches to a different plan, effective immediately. */
    public void changePlanTo(Plan newPlan) {
        this.plan = newPlan;
    }

    /** Cancels the subscription immediately (status becomes CANCELED). */
    public void cancel(Instant when) {
        this.status = SubscriptionStatus.CANCELED;
        this.canceledAt = when;
    }

    public boolean isLive() {
        return status.isLive();
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return userId.equals(candidateUserId);
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserId() {
        return userId;
    }

    public Plan getPlan() {
        return plan;
    }

    public SubscriptionStatus getStatus() {
        return status;
    }

    public Instant getCurrentPeriodStart() {
        return currentPeriodStart;
    }

    public Instant getCurrentPeriodEnd() {
        return currentPeriodEnd;
    }

    public boolean isCancelAtPeriodEnd() {
        return cancelAtPeriodEnd;
    }

    public Instant getCanceledAt() {
        return canceledAt;
    }

    public String getStripeSubscriptionId() {
        return stripeSubscriptionId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
