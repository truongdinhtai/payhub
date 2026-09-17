-- Payment transactions (Stripe checkout attempts and their outcomes).
CREATE TABLE transactions (
    id                       UUID         PRIMARY KEY,
    user_id                  UUID         NOT NULL REFERENCES users (id),
    subscription_id          UUID         REFERENCES subscriptions (id),
    stripe_session_id        VARCHAR(255) UNIQUE,
    stripe_payment_intent_id VARCHAR(255) UNIQUE,
    amount_cents             BIGINT       NOT NULL,
    currency                 VARCHAR(3)   NOT NULL,
    status                   VARCHAR(32)  NOT NULL,
    description              VARCHAR(255),
    customer_name            VARCHAR(255),
    customer_email           VARCHAR(320),
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_transactions_user ON transactions (user_id, created_at DESC);
CREATE INDEX idx_transactions_status ON transactions (status);
