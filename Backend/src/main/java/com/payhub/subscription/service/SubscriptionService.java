package com.payhub.subscription.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.service.AuditService;
import com.payhub.common.exception.BusinessRuleException;
import com.payhub.common.exception.ForbiddenResourceException;
import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.payment.service.PaymentService;
import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.domain.Subscription;
import com.payhub.subscription.domain.SubscriptionStatus;
import com.payhub.subscription.repository.SubscriptionRepository;

/**
 * Subscription lifecycle and the rules around it. Every operation is scoped to
 * a {@code userId} (the caller's tenant identity), so users can only act on
 * their own subscriptions.
 *
 * <p>Rules enforced here:
 * <ul>
 *   <li>a user may hold at most one live subscription at a time;</li>
 *   <li>changing to the plan you are already on is rejected;</li>
 *   <li>cancellation is idempotent-safe: you cannot re-schedule a cancellation.</li>
 * </ul>
 */
@Service
public class SubscriptionService {

    private static final String ENTITY_TYPE = "Subscription";

    private final SubscriptionRepository subscriptionRepository;
    private final PlanService planService;
    private final PaymentService paymentService;
    private final AuditService auditService;
    private final Clock clock;

    public SubscriptionService(SubscriptionRepository subscriptionRepository,
                               PlanService planService,
                               PaymentService paymentService,
                               AuditService auditService,
                               Clock clock) {
        this.subscriptionRepository = subscriptionRepository;
        this.planService = planService;
        this.paymentService = paymentService;
        this.auditService = auditService;
        this.clock = clock;
    }

    /**
     * Subscribes the user to a plan. Free plans activate immediately; paid plans
     * create an INCOMPLETE subscription and return a Stripe Checkout URL — they
     * become ACTIVE once the payment webhook fires.
     */
    @Transactional
    public SubscriptionResult subscribe(UUID userId, PlanCode planCode) {
        if (subscriptionRepository.existsByUserIdAndStatusIn(userId, SubscriptionStatus.live())) {
            throw new BusinessRuleException(
                    "User already has an active subscription; change or cancel it instead");
        }
        Plan plan = planService.getByCode(planCode);

        Instant now = clock.instant();
        Instant periodEnd = now.atZone(ZoneOffset.UTC).plusMonths(1).toInstant();

        boolean free = plan.getPriceCents() == 0;
        Subscription subscription = free
                ? Subscription.start(userId, plan, now, periodEnd)
                : Subscription.startIncomplete(userId, plan, now, periodEnd);
        Subscription saved = subscriptionRepository.save(subscription);

        auditService.record(userId, AuditAction.SUBSCRIPTION_CREATED, ENTITY_TYPE, saved.getId(),
                null, SubscriptionSnapshot.of(saved));

        if (free) {
            return SubscriptionResult.activated(saved);
        }
        String checkoutUrl = paymentService.startCheckout(userId, saved.getId(),
                plan.getPriceCents(), plan.getCurrency(), plan.getName() + " plan",
                plan.getCode().name());
        return SubscriptionResult.checkout(saved, checkoutUrl);
    }

    /**
     * Changes the current subscription's plan. Upgrading to a higher-priced plan
     * requires payment (returns a Stripe Checkout URL; the new plan is applied by
     * the webhook). Downgrading, or moving to an equal/lower-priced plan, applies
     * immediately at no charge.
     */
    @Transactional
    public SubscriptionResult changePlan(UUID userId, PlanCode newPlanCode) {
        Subscription subscription = requireCurrent(userId);
        if (subscription.getPlan().getCode() == newPlanCode) {
            throw new BusinessRuleException("Subscription is already on plan " + newPlanCode);
        }
        Plan newPlan = planService.getByCode(newPlanCode);

        boolean upgrade = newPlan.getPriceCents() > subscription.getPlan().getPriceCents();
        if (upgrade) {
            String checkoutUrl = paymentService.startCheckout(userId, subscription.getId(),
                    newPlan.getPriceCents(), newPlan.getCurrency(), newPlan.getName() + " plan",
                    newPlan.getCode().name());
            return SubscriptionResult.checkout(subscription, checkoutUrl);
        }

        Map<String, Object> before = SubscriptionSnapshot.of(subscription);
        subscription.changePlanTo(newPlan);
        Map<String, Object> after = SubscriptionSnapshot.of(subscription);

        auditService.record(userId, AuditAction.SUBSCRIPTION_PLAN_CHANGED, ENTITY_TYPE,
                subscription.getId(), before, after);
        return SubscriptionResult.activated(subscription);
    }

    /**
     * Applies a paid plan to a subscription after its payment succeeds: sets the
     * plan (which may differ, for an upgrade) and activates it. Called by the
     * Stripe webhook; {@code actorUserId} is the subscriber (audit actor).
     */
    @Transactional
    public void applyPaidPlan(UUID subscriptionId, PlanCode targetPlanCode, UUID actorUserId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
        Plan targetPlan = planService.getByCode(targetPlanCode);

        Map<String, Object> before = SubscriptionSnapshot.of(subscription);
        subscription.changePlanTo(targetPlan);
        subscription.activate();
        Map<String, Object> after = SubscriptionSnapshot.of(subscription);

        auditService.record(actorUserId, AuditAction.SUBSCRIPTION_ACTIVATED, ENTITY_TYPE,
                subscription.getId(), before, after);
    }

    @Transactional
    public Subscription cancel(UUID userId) {
        Subscription subscription = requireCurrent(userId);
        Map<String, Object> before = SubscriptionSnapshot.of(subscription);
        subscription.cancel(clock.instant());
        Map<String, Object> after = SubscriptionSnapshot.of(subscription);

        auditService.record(userId, AuditAction.SUBSCRIPTION_CANCELED, ENTITY_TYPE,
                subscription.getId(), before, after);
        return subscription;
    }

    @Transactional(readOnly = true)
    public Subscription getCurrent(UUID userId) {
        return requireCurrent(userId);
    }

    @Transactional(readOnly = true)
    public List<Subscription> getHistory(UUID userId) {
        return subscriptionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public Subscription getOwned(UUID userId, UUID subscriptionId) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Subscription", subscriptionId));
        if (!subscription.isOwnedBy(userId)) {
            throw new ForbiddenResourceException("Subscription does not belong to the current user");
        }
        return subscription;
    }

    private Subscription requireCurrent(UUID userId) {
        return subscriptionRepository.findByUserIdAndStatusIn(userId, SubscriptionStatus.live())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No active subscription for the current user"));
    }
}
