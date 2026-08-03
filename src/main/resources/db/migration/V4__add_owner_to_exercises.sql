ALTER TABLE exercises
    ADD COLUMN owner_id BIGINT REFERENCES users (user_id) ON DELETE CASCADE;

CREATE INDEX idx_exercises_owner_id ON exercises (owner_id);

ALTER TABLE exercises
    DROP COLUMN is_custom;