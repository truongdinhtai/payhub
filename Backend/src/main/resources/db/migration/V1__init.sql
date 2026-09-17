-- Baseline migration for PayHub.
-- Enables pgcrypto so later migrations can default UUID primary keys with
-- gen_random_uuid(). Feature tables are added in subsequent migrations.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
