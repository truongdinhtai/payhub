package com.payhub.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.service.AuditService;
import com.payhub.payment.service.PaymentService;
import com.payhub.common.exception.BusinessRuleException;
import com.payhub.common.exception.ForbiddenResourceException;
import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.subscription.domain.Plan;
import com.payhub.subscription.domain.PlanCode;
import com.payhub.subscription.domain.Subscription;
import com.payhub.subscription.domain.SubscriptionStatus;
import com.payhub.subscription.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    private static final Instant NOW = Instant.parse("2026-01-15T10:00:00Z");
    private static final UUID USER = UUID.randomUUID();
    private static final UUID OTHER_USER = UUID.randomUUID();

    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private PlanService planService;

    @Mock
    private AuditService auditService;

    @Mock
    private PaymentService paymentService;

    private SubscriptionService service;

    private final Plan freePlan = new Plan(PlanCode.FREE, "Free", "d", 0, "USD", 1, 1);
    private final Plan proPlan = new Plan(PlanCode.PRO, "Pro", "d", 2900, "USD", 10, 5);

    private SubscriptionService service() {
        if (service == null) {
            service = new SubscriptionService(subscriptionRepository, planService, paymentService,
                    auditService, clock);
        }
        return service;
    }

    // ---- subscribe ----

    @Test
    void subscribe_toFreePlan_activatesImmediatelyWithoutCheckout() {
        when(subscriptionRepository.existsByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(false);
        when(planService.getByCode(PlanCode.FREE)).thenReturn(freePlan);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        SubscriptionResult result = service().subscribe(USER, PlanCode.FREE);

        assertThat(result.checkoutUrl()).isNull();
        assertThat(result.subscription().getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(result.subscription().getCurrentPeriodStart()).isEqualTo(NOW);
        assertThat(result.subscription().getCurrentPeriodEnd())
                .isEqualTo(Instant.parse("2026-02-15T10:00:00Z"));
        verify(auditService).record(eq(USER), eq(AuditAction.SUBSCRIPTION_CREATED),
                eq("Subscription"), any(), isNull(), any());
        verify(paymentService, never()).startCheckout(any(), any(), anyLong(), any(), any(), any());
    }

    @Test
    void subscribe_toPaidPlan_createsIncompleteAndReturnsCheckoutUrl() {
        when(subscriptionRepository.existsByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(false);
        when(planService.getByCode(PlanCode.PRO)).thenReturn(proPlan);
        when(subscriptionRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(paymentService.startCheckout(eq(USER), any(), eq(2900L), eq("USD"), eq("Pro plan"), eq("PRO")))
                .thenReturn("https://checkout.stripe/session");

        SubscriptionResult result = service().subscribe(USER, PlanCode.PRO);

        assertThat(result.subscription().getStatus()).isEqualTo(SubscriptionStatus.INCOMPLETE);
        assertThat(result.checkoutUrl()).isEqualTo("https://checkout.stripe/session");
        verify(paymentService).startCheckout(eq(USER), any(), eq(2900L), eq("USD"), eq("Pro plan"), eq("PRO"));
    }

    @Test
    void subscribe_rejects_whenUserAlreadyHasLiveSubscription() {
        when(subscriptionRepository.existsByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(true);

        assertThatThrownBy(() -> service().subscribe(USER, PlanCode.PRO))
                .isInstanceOf(BusinessRuleException.class);

        verify(subscriptionRepository, never()).save(any());
        verify(auditService, never()).record(any(), any(), any(), any(), any(), any());
        verify(paymentService, never()).startCheckout(any(), any(), anyLong(), any(), any(), any());
    }

    // ---- changePlan ----

    @Test
    void changePlan_upgrade_returnsCheckoutUrlWithoutChangingPlanYet() {
        Subscription current = Subscription.start(USER, freePlan, NOW, NOW);
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.of(current));
        when(planService.getByCode(PlanCode.PRO)).thenReturn(proPlan);
        when(paymentService.startCheckout(eq(USER), any(), eq(2900L), eq("USD"), eq("Pro plan"), eq("PRO")))
                .thenReturn("https://checkout/upgrade");

        SubscriptionResult result = service().changePlan(USER, PlanCode.PRO);

        assertThat(result.checkoutUrl()).isEqualTo("https://checkout/upgrade");
        assertThat(current.getPlan()).isSameAs(freePlan); // plan applied only by the webhook
        verify(auditService, never()).record(any(), eq(AuditAction.SUBSCRIPTION_PLAN_CHANGED),
                any(), any(), any(), any());
    }

    @Test
    void changePlan_downgrade_appliesImmediatelyWithoutCharge() {
        Subscription current = Subscription.start(USER, proPlan, NOW, NOW);
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.of(current));
        when(planService.getByCode(PlanCode.FREE)).thenReturn(freePlan);

        SubscriptionResult result = service().changePlan(USER, PlanCode.FREE);

        assertThat(result.checkoutUrl()).isNull();
        assertThat(result.subscription().getPlan()).isSameAs(freePlan);
        verify(auditService).record(eq(USER), eq(AuditAction.SUBSCRIPTION_PLAN_CHANGED),
                eq("Subscription"), any(), any(), any());
        verify(paymentService, never()).startCheckout(any(), any(), anyLong(), any(), any(), any());
    }

    @Test
    void changePlan_rejects_whenAlreadyOnThatPlan() {
        Subscription current = Subscription.start(USER, proPlan, NOW, NOW);
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.of(current));

        assertThatThrownBy(() -> service().changePlan(USER, PlanCode.PRO))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void changePlan_rejects_whenNoCurrentSubscription() {
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().changePlan(USER, PlanCode.PRO))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- cancel ----

    @Test
    void cancel_marksSubscriptionCanceledImmediately() {
        Subscription current = Subscription.start(USER, proPlan, NOW, NOW);
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.of(current));

        Subscription result = service().cancel(USER);

        assertThat(result.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(result.getCanceledAt()).isEqualTo(NOW);
        verify(auditService).record(eq(USER), eq(AuditAction.SUBSCRIPTION_CANCELED),
                eq("Subscription"), any(), any(), any());
    }

    @Test
    void cancel_rejects_whenNoCurrentSubscription() {
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().cancel(USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- applyPaidPlan (webhook) ----

    @Test
    void applyPaidPlan_setsPlanAndActivates() {
        Subscription incomplete = Subscription.startIncomplete(USER, freePlan, NOW, NOW);
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(incomplete));
        when(planService.getByCode(PlanCode.PRO)).thenReturn(proPlan);

        service().applyPaidPlan(subId, PlanCode.PRO, USER);

        assertThat(incomplete.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(incomplete.getPlan()).isSameAs(proPlan);
        verify(auditService).record(eq(USER), eq(AuditAction.SUBSCRIPTION_ACTIVATED),
                eq("Subscription"), any(), any(), any());
    }

    // ---- getOwned (tenancy) ----

    @Test
    void getOwned_returnsSubscription_forOwner() {
        Subscription sub = Subscription.start(USER, proPlan, NOW, NOW);
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        assertThat(service().getOwned(USER, subId)).isSameAs(sub);
    }

    @Test
    void getOwned_forbidden_forDifferentUser() {
        Subscription sub = Subscription.start(USER, proPlan, NOW, NOW);
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> service().getOwned(OTHER_USER, subId))
                .isInstanceOf(ForbiddenResourceException.class);
    }

    @Test
    void getOwned_notFound_whenMissing() {
        UUID subId = UUID.randomUUID();
        when(subscriptionRepository.findById(subId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getOwned(USER, subId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---- getCurrent ----

    @Test
    void getCurrent_returnsLiveSubscription() {
        Subscription sub = Subscription.start(USER, proPlan, NOW, NOW);
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.of(sub));

        assertThat(service().getCurrent(USER)).isSameAs(sub);
    }

    @Test
    void getCurrent_notFound_whenNoneLive() {
        when(subscriptionRepository.findByUserIdAndStatusIn(USER, SubscriptionStatus.live()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service().getCurrent(USER))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
