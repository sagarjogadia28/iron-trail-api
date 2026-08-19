package com.irontrail.api.session.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.SessionExerciseRepository
import com.irontrail.api.session.repository.SessionSetRepository
import com.irontrail.api.session.repository.WorkoutSessionRepository
import com.irontrail.api.split.model.SetType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime

class SessionOwnershipResolverTest {

    private val workoutSessionRepository: WorkoutSessionRepository = mock()
    private val sessionExerciseRepository: SessionExerciseRepository = mock()
    private val sessionSetRepository: SessionSetRepository = mock()

    private val resolver = SessionOwnershipResolver(workoutSessionRepository, sessionExerciseRepository, sessionSetRepository)

    private fun session(id: Long = 1L, ownerId: Long = 10L) =
        WorkoutSession(
            ownerId = ownerId,
            startedAt = OffsetDateTime.now(),
            durationSeconds = 0,
            status = SessionStatus.ACTIVE
        ).apply { sessionId = id }

    private fun sessionExercise(id: Long = 1L) =
        SessionExercise(
            exerciseNameSnapshot = "Bench Press",
            inputTypeSnapshot = ExerciseInputType.REPS,
            isRepRange = true,
            restDurationSeconds = 90,
            sortOrder = 0
        ).apply { sessionExerciseId = id }

    private fun sessionSet(id: Long = 1L) =
        SessionSet(
            sortOrder = 0,
            setType = SetType.NORMAL,
            isCompleted = false
        ).apply { sessionSetId = id }

    // ---- getOwnedWorkoutSession ----

    @Test
    fun `getOwnedWorkoutSession returns the session when the repository finds it for that owner`() {
        val expected = session(id = 5L, ownerId = 10L)
        whenever(workoutSessionRepository.findBySessionIdAndOwnerId(5L, 10L)).thenReturn(expected)

        val result = resolver.getOwnedWorkoutSession(5L, 10L)

        assertSame(expected, result)
    }

    @Test
    fun `getOwnedWorkoutSession throws NotFoundException naming WorkoutSession and the requested id when not owned or missing`() {
        whenever(workoutSessionRepository.findBySessionIdAndOwnerId(5L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            resolver.getOwnedWorkoutSession(5L, 10L)
        }
        assertEquals("WorkoutSession not found: 5", ex.message)
    }

    @Test
    fun `getOwnedWorkoutSession passes the exact sessionId and userId through to the repository`() {
        whenever(workoutSessionRepository.findBySessionIdAndOwnerId(eq(7L), eq(20L))).thenReturn(session(7L, 20L))

        resolver.getOwnedWorkoutSession(7L, 20L)

        verify(workoutSessionRepository).findBySessionIdAndOwnerId(7L, 20L)
    }

    // ---- getOwnedSessionExercise ----

    @Test
    fun `getOwnedSessionExercise returns the exercise when the repository finds it for that owner`() {
        val expected = sessionExercise(id = 3L)
        whenever(sessionExerciseRepository.findOwnedBySessionExerciseId(3L, 10L)).thenReturn(expected)

        val result = resolver.getOwnedSessionExercise(3L, 10L)

        assertSame(expected, result)
    }

    @Test
    fun `getOwnedSessionExercise throws NotFoundException naming SessionExercise and the requested id when not owned or missing`() {
        whenever(sessionExerciseRepository.findOwnedBySessionExerciseId(3L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            resolver.getOwnedSessionExercise(3L, 10L)
        }
        assertEquals("SessionExercise not found: 3", ex.message)
    }

    // ---- getOwnedSessionSet ----

    @Test
    fun `getOwnedSessionSet returns the set when the repository finds it for that owner`() {
        val expected = sessionSet(id = 9L)
        whenever(sessionSetRepository.findOwnedBySessionSetId(9L, 10L)).thenReturn(expected)

        val result = resolver.getOwnedSessionSet(9L, 10L)

        assertSame(expected, result)
    }

    @Test
    fun `getOwnedSessionSet throws NotFoundException naming SessionSet and the requested id when not owned or missing`() {
        whenever(sessionSetRepository.findOwnedBySessionSetId(9L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            resolver.getOwnedSessionSet(9L, 10L)
        }
        assertEquals("SessionSet not found: 9", ex.message)
    }

    @Test
    fun `getOwnedSessionSet does not fall back to any other repository when the set is not found`() {
        whenever(sessionSetRepository.findOwnedBySessionSetId(9L, 10L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) { resolver.getOwnedSessionSet(9L, 10L) }

        org.mockito.kotlin.verifyNoInteractions(workoutSessionRepository)
        org.mockito.kotlin.verifyNoInteractions(sessionExerciseRepository)
    }
}
