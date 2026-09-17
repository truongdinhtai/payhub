-- Full-text search over transactions (customer name, email, description).
-- A STORED generated column keeps the tsvector in sync automatically; a GIN
-- index makes the @@ match fast. The 'simple' config is used (no stemming),
-- which suits names and emails better than a language-specific config.
ALTER TABLE transactions
    ADD COLUMN search_vector tsvector
    GENERATED ALWAYS AS (
        to_tsvector('simple',
            coalesce(customer_name, '') || ' ' ||
            coalesce(customer_email, '') || ' ' ||
            coalesce(description, ''))
    ) STORED;

CREATE INDEX idx_transactions_search ON transactions USING GIN (search_vector);
