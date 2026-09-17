-- Subscription plan catalog (reference data).
CREATE TABLE plans (
    code            VARCHAR(32)  PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    description     VARCHAR(500),
    price_cents     INTEGER      NOT NULL,
    currency        VARCHAR(3)   NOT NULL,
    max_projects    INTEGER      NOT NULL,   -- -1 means unlimited
    max_seats       INTEGER      NOT NULL,   -- -1 means unlimited
    stripe_price_id VARCHAR(255)             -- populated in the Stripe step
);

INSERT INTO plans (code, name, description, price_cents, currency, max_projects, max_seats) VALUES
    ('FREE',       'Free',       'For trying things out',          0,    'USD',  1,  1),
    ('PRO',        'Pro',        'For growing teams',              2900, 'USD', 10,  5),
    ('ENTERPRISE', 'Enterprise', 'Unlimited scale and support',    9900, 'USD', -1, -1);

-- Per-user subscriptions.
CREATE TABLE subscriptions (
    id                     UUID         PRIMARY KEY,
    user_id                UUID         NOT NULL REFERENCES users (id),
    plan_code              VARCHAR(32)  NOT NULL REFERENCES plans (code),
    status                 VARCHAR(32)  NOT NULL,
    current_period_start   TIMESTAMPTZ  NOT NULL,
    current_period_end     TIMESTAMPTZ  NOT NULL,
    cancel_at_period_end   BOOLEAN      NOT NULL DEFAULT FALSE,
    canceled_at            TIMESTAMPTZ,
    stripe_subscription_id VARCHAR(255) UNIQUE,
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_subscriptions_user ON subscriptions (user_id);

-- A user may have at most one live subscription at a time. Enforced in the
-- domain, and here at the database level via a partial unique index.
CREATE UNIQUE INDEX uq_active_subscription_per_user
    ON subscriptions (user_id)
    WHERE status IN ('ACTIVE', 'PAST_DUE', 'INCOMPLETE');
