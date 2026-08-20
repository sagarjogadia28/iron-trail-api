package com.irontrail.api.split.repository

import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

class TemplateExerciseRepositoryTest : RepositoryTestBase() {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var templateExerciseRepository: TemplateExerciseRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistSplit(ownerId: Long): Split = entityManager.persistAndFlush(Split(ownerId = ownerId, name = "PPL"))

    private fun persistDay(splitId: Long): WorkoutDay =
        entityManager.persistAndFlush(WorkoutDay(splitId = splitId, name = "Push Day", sortOrder = 0))

    private fun persistExercise(): Long =
        entityManager
            .persistAndFlush(
                Exercise(
                    wgerId = null,
                    name = "Bench Press",
                    primaryMuscleGroup = MuscleGroup.CHEST,
                    secondaryMuscleGroups = emptyList(),
                    equipment = Equipment.BARBELL,
                    inputType = ExerciseInputType.REPS,
                    description = null,
                    imageUrl = null,
                    ownerId = null,
                ),
            ).exerciseId

    private fun persistTemplateExercise(
        workoutDayId: Long,
        exerciseId: Long = persistExercise(),
        sortOrder: Int = 0,
    ): TemplateExercise =
        entityManager.persistAndFlush(
            TemplateExercise(workoutDayId = workoutDayId, exerciseId = exerciseId, sortOrder = sortOrder),
        )

    // ---- findByWorkoutDayIdIn ----

    @Test
    fun `findByWorkoutDayIdIn returns exercises for every requested day id`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val dayA = persistDay(split.splitId)
        val dayB = persistDay(split.splitId)
        val exA = persistExercise()
        val exB = persistExercise()
        persistTemplateExercise(dayA.workoutDayId, exerciseId = exA)
        persistTemplateExercise(dayB.workoutDayId, exerciseId = exB)

        val result = templateExerciseRepository.findByWorkoutDayIdIn(listOf(dayA.workoutDayId, dayB.workoutDayId))

        assertEquals(setOf(exA, exB), result.map { it.exerciseId }.toSet())
    }

    @Test
    fun `findByWorkoutDayIdIn excludes exercises under a day not in the requested ids`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val dayA = persistDay(split.splitId)
        val dayB = persistDay(split.splitId)
        val exA = persistExercise()
        val exB = persistExercise()
        persistTemplateExercise(dayA.workoutDayId, exerciseId = exA)
        persistTemplateExercise(dayB.workoutDayId, exerciseId = exB)

        val result = templateExerciseRepository.findByWorkoutDayIdIn(listOf(dayA.workoutDayId))

        assertEquals(listOf(exA), result.map { it.exerciseId })
    }

    // ---- findOwnedByTemplateExerciseId ----

    @Test
    fun `findOwnedByTemplateExerciseId returns the exercise when the split, two levels up, is owned by the caller`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val ex = persistExercise()
        val te = persistTemplateExercise(day.workoutDayId, exerciseId = ex)

        val result = templateExerciseRepository.findOwnedByTemplateExerciseId(te.templateExerciseId, owner)

        assertEquals(ex, result?.exerciseId)
    }

    @Test
    fun `findOwnedByTemplateExerciseId returns null when the owning split belongs to someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val te = persistTemplateExercise(day.workoutDayId)

        val result = templateExerciseRepository.findOwnedByTemplateExerciseId(te.templateExerciseId, stranger)

        assertNull(result)
    }

    @Test
    fun `findOwnedByTemplateExerciseId returns null for a non-existent id`() {
        val owner = persistUser()

        val result = templateExerciseRepository.findOwnedByTemplateExerciseId(999_999L, owner)

        assertNull(result)
    }

    @Test
    fun `findOwnedByTemplateExerciseId returns null for another owner's day`() {
        // Two different owners, each with their own split/day/exercise - proves the join genuinely
        // filters on ownership rather than just matching on any split row existing.
        val ownerA = persistUser()
        val ownerB = persistUser()
        val splitA = persistSplit(ownerA)
        val splitB = persistSplit(ownerB)
        val dayA = persistDay(splitA.splitId)
        val dayB = persistDay(splitB.splitId)
        val teA = persistTemplateExercise(dayA.workoutDayId, exerciseId = persistExercise())
        persistTemplateExercise(dayB.workoutDayId, exerciseId = persistExercise())

        val result = templateExerciseRepository.findOwnedByTemplateExerciseId(teA.templateExerciseId, ownerB)

        assertNull(result)
    }
}
