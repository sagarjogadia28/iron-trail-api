CREATE TABLE exercises
(
    exercise_id BIGSERIAL PRIMARY KEY,
    wger_id     INTEGER,
    name        VARCHAR(255) NOT NULL,
    equipment   VARCHAR(50)  NOT NULL,
    input_type  VARCHAR(20)  NOT NULL,
    description TEXT,
    image_url   VARCHAR(500),
    is_custom   BOOLEAN      NOT NULL
);

CREATE TABLE exercise_muscle_groups
(
    exercise_id  BIGINT      NOT NULL REFERENCES exercises (exercise_id) ON DELETE CASCADE,
    muscle_group VARCHAR(50) NOT NULL,
    PRIMARY KEY (exercise_id, muscle_group)
);