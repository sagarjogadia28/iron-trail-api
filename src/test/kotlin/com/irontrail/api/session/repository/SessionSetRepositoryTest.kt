package com.irontrail.api.session.repository

import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.split.model.SetType
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import java.time.OffsetDateTime

class SessionSetRepositoryTest : RepositoryTestBase() {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var sessionSetRepository: SessionSetRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistSession(ownerId: Long): WorkoutSession =
        entityManager.persistAndFlush(
            WorkoutSession(ownerId = ownerId, startedAt = OffsetDateTime.now(), durationSeconds = 0, status = SessionStatus.COMPLETED),
        )

    private fun persistSessionExercise(session: WorkoutSession): SessionExercise =
        entityManager.persistAndFlush(
            SessionExercise(
                exerciseNameSnapshot = "Bench Press",
                inputTypeSnapshot = ExerciseInputType.REPS,
                isRepRange = true,
                restDurationSeconds = 90,
                sortOrder = 0,
            ).apply { workoutSession = session },
        )

    private fun persistSessionSet(
        parent: SessionExercise,
        reps: Int? = 8,
    ): SessionSet =
        entityManager.persistAndFlush(
            SessionSet(sortOrder = 0, setType = SetType.NORMAL, reps = reps, isCompleted = true).apply { sessionExercise = parent },
        )

    // ---- findBySessionExerciseIn ----

    @Test
    fun `findBySessionExerciseIn returns sets belonging to any of the given exercises`() {
        val owner = persistUser()
        val session = persistSession(owner)
        val exA = persistSessionExercise(session)
        val exB = persistSessionExercise(session)
        persistSessionSet(exA, reps = 8)
        persistSessionSet(exB, reps = 12)

        val result = sessionSetRepository.findBySessionExerciseIn(listOf(exA, exB))

        assertEquals(setOf(8, 12), result.map { it.reps }.toSet())
    }

    @Test
    fun `findBySessionExerciseIn excludes sets belonging to an exercise not in the given list`() {
        val owner = persistUser()
        val session = persistSession(owner)
        val exA = persistSessionExercise(session)
        val exB = persistSessionExercise(session)
        persistSessionSet(exA, reps = 8)
        persistSessionSet(exB, reps = 12)

        val result = sessionSetRepository.findBySessionExerciseIn(listOf(exA))

        assertEquals(listOf(8), result.map { it.reps })
    }

    @Test
    fun `findBySessionExerciseIn returns an empty list for an empty exercise list`() {
        assertTrue(sessionSetRepository.findBySessionExerciseIn(emptyList()).isEmpty())
    }

    // ---- findOwnedBySessionSetId ----

    @Test
    fun `findOwnedBySessionSetId returns the set when the session, two levels up, is owned by the caller`() {
        val owner = persistUser()
        val session = persistSession(owner)
        val ex = persistSessionExercise(session)
        val set = persistSessionSet(ex, reps = 15)

        val result = sessionSetRepository.findOwnedBySessionSetId(set.sessionSetId, owner)

        assertEquals(15, result?.reps)
    }

    @Test
    fun `findOwnedBySessionSetId returns null when the owning session belongs to someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val session = persistSession(owner)
        val ex = persistSessionExercise(session)
        val set = persistSessionSet(ex)

        assertNull(sessionSetRepository.findOwnedBySessionSetId(set.sessionSetId, stranger))
    }

    @Test
    fun `findOwnedBySessionSetId returns null for a non-existent id`() {
        val owner = persistUser()

        assertNull(sessionSetRepository.findOwnedBySessionSetId(999_999L, owner))
    }
}
