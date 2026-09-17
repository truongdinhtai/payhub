package com.payhub.payment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.service.AuditService;
import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.payment.domain.Transaction;
import com.payhub.payment.domain.TransactionStatus;
import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.CheckoutSession;
import com.payhub.payment.repository.TransactionRepository;
import com.payhub.support.TestUsers;
import com.payhub.user.domain.Role;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private StripeGateway stripeGateway;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;

    private PaymentService paymentService() {
        return new PaymentService(transactionRepository, stripeGateway, userService, auditService);
    }

    private static Transaction pendingTransaction(String sessionId) {
        Transaction transaction = Transaction.pending(UUID.randomUUID(), UUID.randomUUID(),
                2900L, "USD", "Pro plan", "Buyer", "buyer@example.com", "PRO");
        transaction.attachStripeSession(sessionId);
        return transaction;
    }

    @Test
    void startCheckout_recordsPendingTransactionAndReturnsUrl() {
        UUID userId = UUID.randomUUID();
        UUID subscriptionId = UUID.randomUUID();
        User user = TestUsers.user(userId, "buyer@example.com", "Buyer", Role.USER);
        when(userService.getById(userId)).thenReturn(user);
        when(stripeGateway.createCheckoutSession(any()))
                .thenReturn(new CheckoutSession("sess_123", "https://stripe/checkout"));

        String url = paymentService().startCheckout(userId, subscriptionId, 2900L, "USD", "Pro plan", "PRO");

        assertThat(url).isEqualTo("https://stripe/checkout");

        ArgumentCaptor<Transaction> captor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(captor.capture());
        Transaction saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(TransactionStatus.PENDING);
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getSubscriptionId()).isEqualTo(subscriptionId);
        assertThat(saved.getAmountCents()).isEqualTo(2900L);
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getDescription()).isEqualTo("Pro plan");
        assertThat(saved.getCustomerName()).isEqualTo("Buyer");
        assertThat(saved.getCustomerEmail()).isEqualTo("buyer@example.com");
        assertThat(saved.getStripeSessionId()).isEqualTo("sess_123");
        assertThat(saved.getTargetPlanCode()).isEqualTo("PRO");
    }

    @Test
    void completeCheckout_marksSucceededAndAudits() {
        Transaction transaction = pendingTransaction("sess_ok");
        when(transactionRepository.findByStripeSessionId("sess_ok"))
                .thenReturn(Optional.of(transaction));

        Transaction result = paymentService().completeCheckout("sess_ok", "pi_1");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.SUCCEEDED);
        assertThat(result.getStripePaymentIntentId()).isEqualTo("pi_1");
        verify(auditService).record(eq(transaction.getUserId()), eq(AuditAction.PAYMENT_SUCCEEDED),
                eq("Transaction"), any(), any(), any());
    }

    @Test
    void failCheckout_marksFailedAndAudits() {
        Transaction transaction = pendingTransaction("sess_bad");
        when(transactionRepository.findByStripeSessionId("sess_bad"))
                .thenReturn(Optional.of(transaction));

        Transaction result = paymentService().failCheckout("sess_bad");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.FAILED);
        verify(auditService).record(eq(transaction.getUserId()), eq(AuditAction.PAYMENT_FAILED),
                eq("Transaction"), any(), any(), any());
    }

    @Test
    void completeCheckout_throws_whenSessionUnknown() {
        when(transactionRepository.findByStripeSessionId("nope")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService().completeCheckout("nope", "pi"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
