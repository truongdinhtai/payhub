-- Deduplication ledger for Stripe webhook delivery. Stripe may deliver the same
-- event more than once; the UNIQUE constraint on stripe_event_id makes
-- processing idempotent.
CREATE TABLE processed_webhook_event (
    id              UUID         PRIMARY KEY,
    stripe_event_id VARCHAR(255) NOT NULL UNIQUE,
    event_type      VARCHAR(64)  NOT NULL,
    processed_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
