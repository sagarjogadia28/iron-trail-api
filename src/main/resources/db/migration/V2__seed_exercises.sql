INSERT INTO exercises (wger_id, name, equipment, input_type, description, image_url, is_custom)
VALUES
    (NULL, 'Bench Press', 'BARBELL', 'REPS', 'Flat barbell bench press', NULL, false),
    (NULL, 'Back Squat', 'BARBELL', 'REPS', 'Barbell back squat', NULL, false),
    (NULL, 'Plank', 'BODYWEIGHT', 'TIMED', 'Hold a plank position', NULL, false),
    (NULL, 'Pull-up', 'BODYWEIGHT', 'REPS', 'Bodyweight pull-up', NULL, false);

INSERT INTO exercise_muscle_groups (exercise_id, muscle_group)
SELECT exercise_id, 'CHEST' FROM exercises WHERE name = 'Bench Press'
UNION ALL
SELECT exercise_id, 'TRICEPS' FROM exercises WHERE name = 'Bench Press'
UNION ALL
SELECT exercise_id, 'QUADS' FROM exercises WHERE name = 'Back Squat'
UNION ALL
SELECT exercise_id, 'GLUTES' FROM exercises WHERE name = 'Back Squat'
UNION ALL
SELECT exercise_id, 'CORE' FROM exercises WHERE name = 'Plank'
UNION ALL
SELECT exercise_id, 'BACK' FROM exercises WHERE name = 'Pull-up'
UNION ALL
SELECT exercise_id, 'BICEPS' FROM exercises WHERE name = 'Pull-up';