package com.payhub.subscription.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.payhub.subscription.domain.Subscription;
import com.payhub.subscription.domain.SubscriptionStatus;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

    /** The user's current (live) subscription, if any. */
    Optional<Subscription> findByUserIdAndStatusIn(UUID userId, Iterable<SubscriptionStatus> statuses);

    boolean existsByUserIdAndStatusIn(UUID userId, Iterable<SubscriptionStatus> statuses);

    /** Full subscription history for a user, newest first. */
    List<Subscription> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
