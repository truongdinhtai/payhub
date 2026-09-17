-- The plan a transaction pays for. Needed so the webhook can apply the correct
-- plan when a paid upgrade completes (the subscription may still be on the old
-- plan until payment succeeds).
ALTER TABLE transactions
    ADD COLUMN target_plan_code VARCHAR(32) REFERENCES plans (code);
