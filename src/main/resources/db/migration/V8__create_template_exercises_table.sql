CREATE TABLE template_exercises
(
    template_exercise_id  BIGSERIAL PRIMARY KEY,
    workout_day_id        BIGINT  NOT NULL REFERENCES workout_days (workout_day_id) ON DELETE CASCADE,
    exercise_id           BIGINT  NOT NULL REFERENCES exercises (exercise_id) ON DELETE CASCADE,
    sort_order            INTEGER NOT NULL,
    rest_duration_seconds INTEGER NOT NULL DEFAULT 90,
    is_rep_range          BOOLEAN NOT NULL DEFAULT true,
    notes                 TEXT
);

CREATE INDEX idx_template_exercises_workout_day_id ON template_exercises (workout_day_id);
CREATE INDEX idx_template_exercises_exercise_id ON template_exercises (exercise_id);