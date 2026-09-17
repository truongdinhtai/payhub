package com.payhub.payment.gateway;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.payhub.payment.exception.PaymentGatewayException;
import com.payhub.payment.exception.WebhookSignatureException;

/**
 * Stripe-backed {@link StripeGateway}. Uses one-time PAYMENT mode with inline
 * price data (representing the first billing period) to keep the demo free of
 * pre-created Stripe Prices. The internal user/subscription ids are attached as
 * metadata so the webhook can correlate the outcome. The global {@code
 * Stripe.apiKey} is initialised in {@code StripeConfig}.
 */
@Component
public class StripeCheckoutGateway implements StripeGateway {

    private final String successUrl;
    private final String cancelUrl;
    private final String webhookSecret;

    public StripeCheckoutGateway(
            @Value("${payhub.stripe.success-url:http://localhost:4200/billing/success}") String successUrl,
            @Value("${payhub.stripe.cancel-url:http://localhost:4200/billing/cancel}") String cancelUrl,
            @Value("${payhub.stripe.webhook-secret:whsec_placeholder}") String webhookSecret) {
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
        this.webhookSecret = webhookSecret;
    }

    @Override
    public CheckoutSession createCheckoutSession(CheckoutCommand command) {
        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .setCustomerEmail(command.customerEmail())
                .putMetadata("userId", String.valueOf(command.userId()))
                .putMetadata("subscriptionId", String.valueOf(command.subscriptionId()))
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency(command.currency())
                                .setUnitAmount(command.amountCents())
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(command.productName())
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            Session session = Session.create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw new PaymentGatewayException("Failed to create Stripe checkout session", e);
        }
    }

    @Override
    public WebhookEvent constructEvent(String payload, String signatureHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new WebhookSignatureException("Invalid Stripe webhook signature", e);
        }

        WebhookEventType type = switch (event.getType()) {
            case "checkout.session.completed" -> WebhookEventType.CHECKOUT_COMPLETED;
            case "checkout.session.expired" -> WebhookEventType.CHECKOUT_EXPIRED;
            default -> WebhookEventType.UNKNOWN;
        };

        String sessionId = null;
        String paymentIntentId = null;
        StripeObject object = event.getDataObjectDeserializer().getObject().orElse(null);
        if (object instanceof Session session) {
            sessionId = session.getId();
            paymentIntentId = session.getPaymentIntent();
        }
        return new WebhookEvent(event.getId(), type, sessionId, paymentIntentId);
    }
}
