package com.irontrail.api.exercise.repository

import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

// These custom @Query methods have twice caused real Hibernate/Postgres bugs that a Mockito unit
// test (or an H2-backed test) would never surface - see ExerciseService's search/filter history in
// CLAUDE.md. This suite runs against a real Postgres container specifically to catch that class of bug.
class ExerciseRepositoryTest : RepositoryTestBase() {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var exerciseRepository: ExerciseRepository

    private fun persistUser(email: String = "user-${System.nanoTime()}@test.com"): Long =
        entityManager.persistAndFlush(User(email = email, passwordHash = "hash")).userId

    private fun persistExercise(
        name: String = "Bench Press",
        primary: MuscleGroup = MuscleGroup.CHEST,
        secondary: List<MuscleGroup> = emptyList(),
        ownerId: Long? = null,
        equipment: Equipment = Equipment.BARBELL,
        inputType: ExerciseInputType = ExerciseInputType.REPS,
    ): Exercise =
        entityManager.persistAndFlush(
            Exercise(
                wgerId = null,
                name = name,
                primaryMuscleGroup = primary,
                secondaryMuscleGroups = secondary,
                equipment = equipment,
                inputType = inputType,
                description = null,
                imageUrl = null,
                ownerId = ownerId,
            ),
        )

    // ---- findVisibleBySearchAndMuscleGroups: visibility ----

    @Test
    fun `findVisibleBySearchAndMuscleGroups includes global exercises for any caller`() {
        persistExercise(name = "Global Squat", ownerId = null)

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("", false, emptyList(), 999L)

        assertTrue(result.any { it.name == "Global Squat" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups includes the caller's own exercises`() {
        val ownerId = persistUser()
        persistExercise(name = "My Custom Curl", ownerId = ownerId)

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("", false, emptyList(), ownerId)

        assertTrue(result.any { it.name == "My Custom Curl" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups excludes another user's private exercises`() {
        val owner = persistUser()
        val stranger = persistUser()
        persistExercise(name = "Owner's Private Move", ownerId = owner)

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("", false, emptyList(), stranger)

        assertTrue(result.none { it.name == "Owner's Private Move" })
    }

    // ---- findVisibleBySearchAndMuscleGroups: search ----

    @Test
    fun `findVisibleBySearchAndMuscleGroups matches a case-insensitive substring of the name`() {
        persistExercise(name = "Barbell Bench Press")
        val caller = persistUser()

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("bench", false, emptyList(), caller)

        assertTrue(result.any { it.name == "Barbell Bench Press" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups excludes names that don't contain the search term`() {
        persistExercise(name = "Deadlift")
        val caller = persistUser()

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("bench", false, emptyList(), caller)

        assertTrue(result.none { it.name == "Deadlift" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups treats an escaped percent sign as a literal character, not a wildcard`() {
        // ExerciseService pre-escapes the raw '%' to '\%' before calling this query; passing the
        // escaped form here proves the ESCAPE '\' clause is doing its job, not just that '%' happens
        // to be a wildcard that matches everything anyway.
        persistExercise(name = "50% Effort Day")
        persistExercise(name = "50X Effort Day")
        val caller = persistUser()

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("50\\%", false, emptyList(), caller)

        assertEquals(listOf("50% Effort Day"), result.map { it.name })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups with a blank search matches every visible exercise`() {
        persistExercise(name = "Anything At All")
        val caller = persistUser()

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("", false, emptyList(), caller)

        assertTrue(result.any { it.name == "Anything At All" })
    }

    // ---- findVisibleBySearchAndMuscleGroups: muscle group filter ----

    @Test
    fun `findVisibleBySearchAndMuscleGroups with hasMuscleFilter false and an empty group list still matches everything`() {
        // Regression coverage for the original JPQL bug: ':muscleGroups IS EMPTY' failed at
        // context-init because a bound List parameter isn't a mapped collection path. The fix
        // replaced it with this boolean flag - this proves an empty list no longer breaks the query.
        persistExercise(name = "Unfiltered Move", primary = MuscleGroup.CALVES)
        val caller = persistUser()

        val result = exerciseRepository.findVisibleBySearchAndMuscleGroups("", false, emptyList(), caller)

        assertTrue(result.any { it.name == "Unfiltered Move" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups matches on primary muscle group`() {
        persistExercise(name = "Squat", primary = MuscleGroup.QUADS)
        val caller = persistUser()

        val result =
            exerciseRepository.findVisibleBySearchAndMuscleGroups(
                "",
                true,
                listOf(MuscleGroup.QUADS),
                caller,
            )

        assertTrue(result.any { it.name == "Squat" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups matches on secondary muscle group`() {
        persistExercise(name = "Bench Press", primary = MuscleGroup.CHEST, secondary = listOf(MuscleGroup.TRICEPS))
        val caller = persistUser()

        val result =
            exerciseRepository.findVisibleBySearchAndMuscleGroups(
                "",
                true,
                listOf(MuscleGroup.TRICEPS),
                caller,
            )

        assertTrue(result.any { it.name == "Bench Press" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups excludes exercises matching neither primary nor secondary group`() {
        persistExercise(name = "Bicep Curl", primary = MuscleGroup.BICEPS, secondary = listOf(MuscleGroup.FOREARMS))
        val caller = persistUser()

        val result =
            exerciseRepository.findVisibleBySearchAndMuscleGroups(
                "",
                true,
                listOf(MuscleGroup.QUADS),
                caller,
            )

        assertTrue(result.none { it.name == "Bicep Curl" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups ORs multiple requested muscle groups - matching any is enough`() {
        persistExercise(name = "Plank", primary = MuscleGroup.CORE)
        val caller = persistUser()

        val result =
            exerciseRepository.findVisibleBySearchAndMuscleGroups(
                "",
                true,
                listOf(MuscleGroup.CORE, MuscleGroup.GLUTES),
                caller,
            )

        assertTrue(result.any { it.name == "Plank" })
    }

    @Test
    fun `findVisibleBySearchAndMuscleGroups ANDs search with the muscle group filter`() {
        persistExercise(name = "Bench Press", primary = MuscleGroup.CHEST)
        persistExercise(name = "Squat", primary = MuscleGroup.QUADS)
        val caller = persistUser()

        val result =
            exerciseRepository.findVisibleBySearchAndMuscleGroups(
                "bench",
                true,
                listOf(MuscleGroup.QUADS),
                caller,
            )

        assertTrue(result.isEmpty())
    }

    // ---- findVisibleById ----

    @Test
    fun `findVisibleById returns a global exercise regardless of caller`() {
        val ex = persistExercise(name = "Global Deadlift", ownerId = null)

        val result = exerciseRepository.findVisibleById(ex.exerciseId, 999L)

        assertEquals("Global Deadlift", result?.name)
    }

    @Test
    fun `findVisibleById returns the caller's own exercise`() {
        val owner = persistUser()
        val ex = persistExercise(name = "My Move", ownerId = owner)

        val result = exerciseRepository.findVisibleById(ex.exerciseId, owner)

        assertEquals("My Move", result?.name)
    }

    @Test
    fun `findVisibleById returns null for another user's private exercise`() {
        val owner = persistUser()
        val stranger = persistUser()
        val ex = persistExercise(name = "Private Move", ownerId = owner)

        val result = exerciseRepository.findVisibleById(ex.exerciseId, stranger)

        assertNull(result)
    }

    @Test
    fun `findVisibleById returns null for a non-existent id`() {
        val caller = persistUser()

        val result = exerciseRepository.findVisibleById(999_999L, caller)

        assertNull(result)
    }

    // ---- existsVisibleById ----

    @Test
    fun `existsVisibleById is true for a global exercise`() {
        val ex = persistExercise(ownerId = null)

        assertTrue(exerciseRepository.existsVisibleById(ex.exerciseId, 999L))
    }

    @Test
    fun `existsVisibleById is true for the caller's own exercise`() {
        val owner = persistUser()
        val ex = persistExercise(ownerId = owner)

        assertTrue(exerciseRepository.existsVisibleById(ex.exerciseId, owner))
    }

    @Test
    fun `existsVisibleById is false for another user's private exercise`() {
        val owner = persistUser()
        val stranger = persistUser()
        val ex = persistExercise(ownerId = owner)

        assertFalse(exerciseRepository.existsVisibleById(ex.exerciseId, stranger))
    }

    @Test
    fun `existsVisibleById is false for a non-existent id`() {
        val caller = persistUser()

        assertFalse(exerciseRepository.existsVisibleById(999_999L, caller))
    }

    // ---- findByExerciseIdAndOwnerId ----

    @Test
    fun `findByExerciseIdAndOwnerId returns the exercise when owned by exactly that caller`() {
        val owner = persistUser()
        val ex = persistExercise(name = "Owned Move", ownerId = owner)

        val result = exerciseRepository.findByExerciseIdAndOwnerId(ex.exerciseId, owner)

        assertEquals("Owned Move", result?.name)
    }

    @Test
    fun `findByExerciseIdAndOwnerId returns null for another user's exercise, even though it exists`() {
        val owner = persistUser()
        val stranger = persistUser()
        val ex = persistExercise(ownerId = owner)

        val result = exerciseRepository.findByExerciseIdAndOwnerId(ex.exerciseId, stranger)

        assertNull(result)
    }

    @Test
    fun `findByExerciseIdAndOwnerId returns null for a global exercise, even for a real user id - it has no owner to match`() {
        val caller = persistUser()
        val ex = persistExercise(name = "Global Move", ownerId = null)

        val result = exerciseRepository.findByExerciseIdAndOwnerId(ex.exerciseId, caller)

        assertNull(result)
    }
}
