CREATE TABLE workout_sessions
(
    session_id                BIGSERIAL PRIMARY KEY,
    owner_id                  BIGINT      NOT NULL REFERENCES users (user_id) ON DELETE CASCADE,
    workout_day_id            BIGINT      REFERENCES workout_days (workout_day_id) ON DELETE SET NULL,
    split_name_snapshot       VARCHAR(255),
    workout_day_name_snapshot VARCHAR(255),
    started_at                TIMESTAMPTZ NOT NULL,
    ended_at                  TIMESTAMPTZ,
    duration_seconds          BIGINT      NOT NULL,
    total_volume_kg           DOUBLE PRECISION,
    completed_sets            INTEGER,
    total_sets                INTEGER,
    notes                     TEXT,
    status                    VARCHAR(20) NOT NULL
);

CREATE INDEX idx_workout_sessions_owner_id ON workout_sessions (owner_id);
CREATE INDEX idx_workout_sessions_workout_day_id ON workout_sessions (workout_day_id);
CREATE UNIQUE INDEX idx_one_active_session_per_owner ON workout_sessions (owner_id) WHERE status IN ('ACTIVE', 'PAUSED');