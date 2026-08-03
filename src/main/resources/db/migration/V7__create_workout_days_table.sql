CREATE TABLE workout_days
(
    workout_day_id BIGSERIAL PRIMARY KEY,
    split_id       BIGINT       NOT NULL REFERENCES splits (split_id) ON DELETE CASCADE,
    name           VARCHAR(255) NOT NULL,
    sort_order     INTEGER      NOT NULL
);

CREATE INDEX idx_workout_days_split_id ON workout_days (split_id);