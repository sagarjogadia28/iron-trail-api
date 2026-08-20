package com.irontrail.api.split.repository

import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.split.model.SetType
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

class TemplateSetRepositoryTest : RepositoryTestBase() {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var templateSetRepository: TemplateSetRepository

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

    private fun persistTemplateExercise(workoutDayId: Long): TemplateExercise =
        entityManager.persistAndFlush(TemplateExercise(workoutDayId = workoutDayId, exerciseId = persistExercise(), sortOrder = 0))

    private fun persistTemplateSet(
        parent: TemplateExercise,
        targetReps: Int? = 8,
    ): TemplateSet =
        entityManager.persistAndFlush(
            TemplateSet(sortOrder = 0, targetReps = targetReps, setType = SetType.NORMAL).apply { templateExercise = parent },
        )

    // ---- findByTemplateExerciseIn ----

    @Test
    fun `findByTemplateExerciseIn returns sets belonging to any of the given exercises`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val exA = persistTemplateExercise(day.workoutDayId)
        val exB = persistTemplateExercise(day.workoutDayId)
        persistTemplateSet(exA, targetReps = 8)
        persistTemplateSet(exB, targetReps = 12)

        val result = templateSetRepository.findByTemplateExerciseIn(listOf(exA, exB))

        assertEquals(setOf(8, 12), result.map { it.targetReps }.toSet())
    }

    @Test
    fun `findByTemplateExerciseIn excludes sets belonging to an exercise not in the given list`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val exA = persistTemplateExercise(day.workoutDayId)
        val exB = persistTemplateExercise(day.workoutDayId)
        persistTemplateSet(exA, targetReps = 8)
        persistTemplateSet(exB, targetReps = 12)

        val result = templateSetRepository.findByTemplateExerciseIn(listOf(exA))

        assertEquals(listOf(8), result.map { it.targetReps })
    }

    @Test
    fun `findByTemplateExerciseIn returns an empty list for an empty exercise list`() {
        val result = templateSetRepository.findByTemplateExerciseIn(emptyList())

        assertTrue(result.isEmpty())
    }

    // ---- findOwnedByTemplateSetId ----

    @Test
    fun `findOwnedByTemplateSetId returns the set when the split, three levels up, is owned by the caller`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val ex = persistTemplateExercise(day.workoutDayId)
        val set = persistTemplateSet(ex, targetReps = 15)

        val result = templateSetRepository.findOwnedByTemplateSetId(set.templateSetId, owner)

        assertEquals(15, result?.targetReps)
    }

    @Test
    fun `findOwnedByTemplateSetId returns null when the owning split belongs to someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)
        val ex = persistTemplateExercise(day.workoutDayId)
        val set = persistTemplateSet(ex)

        val result = templateSetRepository.findOwnedByTemplateSetId(set.templateSetId, stranger)

        assertNull(result)
    }

    @Test
    fun `findOwnedByTemplateSetId returns null for a non-existent id`() {
        val owner = persistUser()

        val result = templateSetRepository.findOwnedByTemplateSetId(999_999L, owner)

        assertNull(result)
    }
}
