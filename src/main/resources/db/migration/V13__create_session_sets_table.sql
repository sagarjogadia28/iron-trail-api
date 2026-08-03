CREATE TABLE session_sets
(
    session_set_id           BIGSERIAL PRIMARY KEY,
    session_exercise_id      BIGINT      NOT NULL REFERENCES session_exercises (session_exercise_id) ON DELETE CASCADE,
    sort_order                INTEGER     NOT NULL,
    set_type                  VARCHAR(20) NOT NULL,
    target_reps                INTEGER,
    target_reps_max            INTEGER,
    target_duration_seconds    INTEGER,
    reps                        INTEGER,
    weight_kg                   DOUBLE PRECISION,
    duration_seconds            INTEGER,
    is_completed                BOOLEAN     NOT NULL
);

CREATE INDEX idx_session_sets_session_exercise_id ON session_sets (session_exercise_id);
