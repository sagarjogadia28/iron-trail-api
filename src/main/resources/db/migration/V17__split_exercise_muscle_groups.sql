ALTER TABLE exercises
    ADD COLUMN primary_muscle_group VARCHAR(20);

-- Deliberate primary pick for the 4 seed exercises (V2)
UPDATE exercises SET primary_muscle_group = 'CHEST' WHERE name = 'Bench Press';
UPDATE exercises SET primary_muscle_group = 'QUADS' WHERE name = 'Back Squat';
UPDATE exercises SET primary_muscle_group = 'CORE' WHERE name = 'Plank';
UPDATE exercises SET primary_muscle_group = 'BACK' WHERE name = 'Pull-up';

-- Fallback for any other pre-existing exercise (e.g. custom test data):
-- promote the alphabetically-first linked muscle group as primary
UPDATE exercises e
SET primary_muscle_group = (
    SELECT MIN(muscle_group) FROM exercise_muscle_groups WHERE exercise_id = e.exercise_id
)
WHERE primary_muscle_group IS NULL;

ALTER TABLE exercises
    ALTER COLUMN primary_muscle_group SET NOT NULL;

ALTER TABLE exercises
    ADD CONSTRAINT chk_exercises_primary_muscle_group
        CHECK (primary_muscle_group IN
            ('CHEST', 'BACK', 'SHOULDERS', 'BICEPS', 'TRICEPS', 'FOREARMS', 'CORE', 'GLUTES', 'QUADS', 'HAMSTRINGS', 'CALVES'));

CREATE INDEX idx_exercises_primary_muscle_group ON exercises (primary_muscle_group);

-- The row promoted to primary no longer belongs in the multi-value table;
-- whatever remains is secondary-only
DELETE FROM exercise_muscle_groups emg
WHERE EXISTS (
    SELECT 1 FROM exercises e
    WHERE e.exercise_id = emg.exercise_id AND e.primary_muscle_group = emg.muscle_group
);

ALTER TABLE exercise_muscle_groups RENAME TO exercise_secondary_muscle_groups;
