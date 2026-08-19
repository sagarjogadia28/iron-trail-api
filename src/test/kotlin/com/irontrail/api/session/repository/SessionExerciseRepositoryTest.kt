package com.irontrail.api.session.repository

import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import java.time.OffsetDateTime

class SessionExerciseRepositoryTest : RepositoryTestBase() {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var sessionExerciseRepository: SessionExerciseRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistSession(ownerId: Long): WorkoutSession = entityManager.persistAndFlush(
        WorkoutSession(ownerId = ownerId, startedAt = OffsetDateTime.now(), durationSeconds = 0, status = SessionStatus.COMPLETED)
    )

    private fun persistSessionExercise(session: WorkoutSession, name: String = "Bench Press"): SessionExercise =
        entityManager.persistAndFlush(
            SessionExercise(
                exerciseNameSnapshot = name,
                inputTypeSnapshot = ExerciseInputType.REPS,
                isRepRange = true,
                restDurationSeconds = 90,
                sortOrder = 0
            ).apply { workoutSession = session }
        )

    // ---- findByWorkoutSessionIn ----

    @Test
    fun `findByWorkoutSessionIn returns exercises belonging to any of the given sessions`() {
        val owner = persistUser()
        val sessionA = persistSession(owner)
        val sessionB = persistSession(owner)
        persistSessionExercise(sessionA, "Bench Press")
        persistSessionExercise(sessionB, "Squat")

        val result = sessionExerciseRepository.findByWorkoutSessionIn(listOf(sessionA, sessionB))

        assertEquals(setOf("Bench Press", "Squat"), result.map { it.exerciseNameSnapshot }.toSet())
    }

    @Test
    fun `findByWorkoutSessionIn excludes exercises from a session not in the given list`() {
        val owner = persistUser()
        val sessionA = persistSession(owner)
        val sessionB = persistSession(owner)
        persistSessionExercise(sessionA, "Bench Press")
        persistSessionExercise(sessionB, "Squat")

        val result = sessionExerciseRepository.findByWorkoutSessionIn(listOf(sessionA))

        assertEquals(listOf("Bench Press"), result.map { it.exerciseNameSnapshot })
    }

    @Test
    fun `findByWorkoutSessionIn returns an empty list for an empty session list`() {
        assertTrue(sessionExerciseRepository.findByWorkoutSessionIn(emptyList()).isEmpty())
    }

    // ---- findOwnedBySessionExerciseId ----

    @Test
    fun `findOwnedBySessionExerciseId returns the exercise when its session is owned by the caller`() {
        val owner = persistUser()
        val session = persistSession(owner)
        val se = persistSessionExercise(session)

        val result = sessionExerciseRepository.findOwnedBySessionExerciseId(se.sessionExerciseId, owner)

        assertEquals(se.sessionExerciseId, result?.sessionExerciseId)
    }

    @Test
    fun `findOwnedBySessionExerciseId returns null when the owning session belongs to someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val session = persistSession(owner)
        val se = persistSessionExercise(session)

        assertNull(sessionExerciseRepository.findOwnedBySessionExerciseId(se.sessionExerciseId, stranger))
    }

    @Test
    fun `findOwnedBySessionExerciseId returns null for a non-existent id`() {
        val owner = persistUser()

        assertNull(sessionExerciseRepository.findOwnedBySessionExerciseId(999_999L, owner))
    }
}
