CREATE TABLE session_exercises
(
    session_exercise_id    BIGSERIAL PRIMARY KEY,
    session_id             BIGINT       NOT NULL REFERENCES workout_sessions (session_id) ON DELETE CASCADE,
    exercise_id            BIGINT       REFERENCES exercises (exercise_id) ON DELETE SET NULL,
    exercise_name_snapshot VARCHAR(255) NOT NULL,
    input_type_snapshot    VARCHAR(20)  NOT NULL,
    is_rep_range           BOOLEAN      NOT NULL,
    rest_duration_seconds  INTEGER      NOT NULL,
    sort_order             INTEGER      NOT NULL,
    notes                  TEXT
);

CREATE INDEX idx_session_exercises_session_id ON session_exercises (session_id);
CREATE INDEX idx_session_exercises_exercise_id ON session_exercises (exercise_id);