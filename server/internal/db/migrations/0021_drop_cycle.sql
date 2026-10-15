-- +goose Up
-- The cycle and pregnancy features are gone from both apps, so the data they
-- shared is deleted rather than kept around: no client reads it any more, and
-- health data we don't use is health data we shouldn't hold.
DROP TABLE IF EXISTS cycle_shares;
DROP TABLE IF EXISTS pregnancy_shares;
ALTER TABLE users DROP COLUMN IF EXISTS has_cycle;
ALTER TABLE users DROP COLUMN IF EXISTS cycle_share_level;

-- +goose Down
-- Restores the schema only (as 0006–0008, 0013 and 0014 left it); the data
-- dropped above is not recoverable.
ALTER TABLE users ADD COLUMN cycle_share_level text NOT NULL DEFAULT 'off';
ALTER TABLE users ADD COLUMN has_cycle boolean;

CREATE TABLE pregnancy_shares (
    user_id    uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    due_date   date NOT NULL,
    updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE TABLE cycle_shares (
    user_id        uuid PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    phase          text NOT NULL,
    cycle_day      integer,
    period_in_days integer,
    note           text,
    updated_at     timestamptz NOT NULL DEFAULT now()
);
