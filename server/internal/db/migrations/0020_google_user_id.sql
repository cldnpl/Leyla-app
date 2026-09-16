-- +goose Up
-- Google sign-in, alongside the existing password and Apple columns. Nullable
-- and UNIQUE like apple_user_id: one Google account maps to at most one user,
-- and most accounts will never have one.
ALTER TABLE users ADD COLUMN google_user_id text UNIQUE;

-- +goose Down
ALTER TABLE users DROP COLUMN google_user_id;
