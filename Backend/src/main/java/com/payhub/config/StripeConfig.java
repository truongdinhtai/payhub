package com.payhub.config;

import jakarta.annotation.PostConstruct;

import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initialises the Stripe SDK with the secret API key (test mode). The key comes
 * from the {@code STRIPE_SECRET_KEY} environment variable; a harmless
 * placeholder is used when unset so the app still starts in local/test runs
 * (real Stripe calls are only made through the gateway).
 */
@Configuration
public class StripeConfig {

    private final String secretKey;

    public StripeConfig(@Value("${payhub.stripe.secret-key:sk_test_placeholder}") String secretKey) {
        this.secretKey = secretKey;
    }

    @PostConstruct
    void init() {
        Stripe.apiKey = secretKey;
    }
}
