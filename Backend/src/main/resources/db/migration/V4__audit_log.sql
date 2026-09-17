-- Immutable audit trail of important changes (subscriptions, and later payments)
-- for compliance: who did what, when, and the before/after state.
CREATE TABLE audit_log (
    id            UUID        PRIMARY KEY,
    actor_user_id UUID,                       -- null = system action (e.g. Stripe webhook)
    action        VARCHAR(64) NOT NULL,
    entity_type   VARCHAR(64) NOT NULL,
    entity_id     VARCHAR(64) NOT NULL,
    before_value  JSONB,
    after_value   JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_actor  ON audit_log (actor_user_id, created_at DESC);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
