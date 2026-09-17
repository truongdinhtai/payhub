package com.payhub.payment.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.payhub.audit.domain.AuditAction;
import com.payhub.audit.service.AuditService;
import com.payhub.common.exception.ResourceNotFoundException;
import com.payhub.payment.domain.Transaction;
import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.CheckoutCommand;
import com.payhub.payment.gateway.StripeGateway.CheckoutSession;
import com.payhub.payment.repository.TransactionRepository;
import com.payhub.user.domain.User;
import com.payhub.user.service.UserService;

/**
 * Starts Stripe checkouts and reads transaction history. The Stripe call is made
 * outside a database transaction on purpose (no long-held DB transaction across
 * an external HTTP call); the single {@code save} is atomic on its own.
 */
@Service
public class PaymentService {

    private static final String ENTITY_TYPE = "Transaction";

    private final TransactionRepository transactionRepository;
    private final StripeGateway stripeGateway;
    private final UserService userService;
    private final AuditService auditService;

    public PaymentService(TransactionRepository transactionRepository,
                          StripeGateway stripeGateway,
                          UserService userService,
                          AuditService auditService) {
        this.transactionRepository = transactionRepository;
        this.stripeGateway = stripeGateway;
        this.userService = userService;
        this.auditService = auditService;
    }

    /**
     * Opens a Stripe checkout for a paid subscription and records a PENDING
     * transaction. Returns the URL to redirect the customer to.
     */
    public String startCheckout(UUID userId, UUID subscriptionId, long amountCents,
                                String currency, String productName, String targetPlanCode) {
        User user = userService.getById(userId);

        Transaction transaction = Transaction.pending(userId, subscriptionId, amountCents,
                currency, productName, user.getName(), user.getEmail(), targetPlanCode);

        CheckoutSession session = stripeGateway.createCheckoutSession(new CheckoutCommand(
                userId, subscriptionId, amountCents, currency, productName, user.getEmail()));
        transaction.attachStripeSession(session.sessionId());

        transactionRepository.save(transaction);
        return session.checkoutUrl();
    }

    /** Marks the transaction for the given Stripe session as succeeded (webhook). */
    @Transactional
    public Transaction completeCheckout(String stripeSessionId, String stripePaymentIntentId) {
        Transaction transaction = requireBySession(stripeSessionId);
        Map<String, Object> before = TransactionSnapshot.of(transaction);
        transaction.markSucceeded(stripePaymentIntentId);
        Map<String, Object> after = TransactionSnapshot.of(transaction);
        auditService.record(transaction.getUserId(), AuditAction.PAYMENT_SUCCEEDED, ENTITY_TYPE,
                transaction.getId(), before, after);
        return transaction;
    }

    /** Marks the transaction for the given Stripe session as failed (webhook). */
    @Transactional
    public Transaction failCheckout(String stripeSessionId) {
        Transaction transaction = requireBySession(stripeSessionId);
        Map<String, Object> before = TransactionSnapshot.of(transaction);
        transaction.markFailed();
        Map<String, Object> after = TransactionSnapshot.of(transaction);
        auditService.record(transaction.getUserId(), AuditAction.PAYMENT_FAILED, ENTITY_TYPE,
                transaction.getId(), before, after);
        return transaction;
    }

    @Transactional(readOnly = true)
    public List<Transaction> getTransactions(UUID userId) {
        return transactionRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private Transaction requireBySession(String stripeSessionId) {
        return transactionRepository.findByStripeSessionId(stripeSessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("Transaction (session)", stripeSessionId));
    }
}
