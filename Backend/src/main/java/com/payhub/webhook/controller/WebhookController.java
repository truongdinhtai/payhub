package com.payhub.webhook.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.payhub.payment.gateway.StripeGateway;
import com.payhub.payment.gateway.StripeGateway.WebhookEvent;
import com.payhub.webhook.service.WebhookService;

/**
 * Receives Stripe webhooks. This endpoint is public (Stripe is not a
 * JWT-bearing client); trust is established by verifying the signature over the
 * raw request body. Invalid signatures yield 400; a 2xx tells Stripe the event
 * was accepted (duplicates are handled idempotently and also return 200).
 */
@RestController
@RequestMapping("/api/v1/webhooks")
@Tag(name = "Webhooks", description = "Stripe webhook receiver")
public class WebhookController {

    private final StripeGateway stripeGateway;
    private final WebhookService webhookService;

    public WebhookController(StripeGateway stripeGateway, WebhookService webhookService) {
        this.stripeGateway = stripeGateway;
        this.webhookService = webhookService;
    }

    @PostMapping("/stripe")
    @Operation(summary = "Stripe webhook endpoint")
    public ResponseEntity<Void> stripe(@RequestBody String payload,
                                       @RequestHeader("Stripe-Signature") String signature) {
        WebhookEvent event = stripeGateway.constructEvent(payload, signature);
        webhookService.handle(event);
        return ResponseEntity.ok().build();
    }
}
