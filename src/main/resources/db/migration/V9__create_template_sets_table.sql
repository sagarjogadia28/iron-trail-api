CREATE TABLE template_sets
(
    template_set_id         BIGSERIAL PRIMARY KEY,
    template_exercise_id    BIGINT      NOT NULL REFERENCES template_exercises (template_exercise_id) ON DELETE CASCADE,
    sort_order              INTEGER     NOT NULL,
    target_reps             INTEGER,
    target_reps_max         INTEGER,
    target_duration_seconds INTEGER,
    set_type                VARCHAR(20) NOT NULL
);

CREATE INDEX idx_template_sets_template_exercise_id ON template_sets (template_exercise_id);