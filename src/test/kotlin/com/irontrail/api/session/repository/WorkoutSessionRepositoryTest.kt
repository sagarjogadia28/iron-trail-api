package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager
import org.springframework.dao.DataIntegrityViolationException
import java.time.OffsetDateTime

class WorkoutSessionRepositoryTest : RepositoryTestBase() {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var workoutSessionRepository: WorkoutSessionRepository

    @Autowired
    lateinit var splitRepository: SplitRepository

    @Autowired
    lateinit var workoutDayRepository: WorkoutDayRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistWorkoutDay(ownerId: Long): Long {
        val split = splitRepository.save(Split(ownerId = ownerId, name = "PPL"))
        return workoutDayRepository.save(WorkoutDay(splitId = split.splitId, name = "Push Day", sortOrder = 0)).workoutDayId
    }

    private fun persistSession(
        ownerId: Long,
        status: SessionStatus = SessionStatus.COMPLETED,
        workoutDayId: Long? = null,
        startedAt: OffsetDateTime = OffsetDateTime.now()
    ): WorkoutSession = entityManager.persistAndFlush(
        WorkoutSession(
            ownerId = ownerId,
            workoutDayId = workoutDayId,
            startedAt = startedAt,
            durationSeconds = 1800,
            status = status
        )
    )

    // ---- findByOwnerId ----

    @Test
    fun `findByOwnerId returns only sessions owned by that user`() {
        val owner = persistUser()
        val stranger = persistUser()
        persistSession(owner)
        persistSession(stranger)

        val result = workoutSessionRepository.findByOwnerId(owner)

        assertEquals(1, result.size)
        assertEquals(owner, result[0].ownerId)
    }

    @Test
    fun `findByOwnerId returns an empty list for a user with no sessions`() {
        val owner = persistUser()

        assertTrue(workoutSessionRepository.findByOwnerId(owner).isEmpty())
    }

    // ---- findBySessionIdAndOwnerId ----

    @Test
    fun `findBySessionIdAndOwnerId returns the session when owned by exactly that caller`() {
        val owner = persistUser()
        val session = persistSession(owner)

        val result = workoutSessionRepository.findBySessionIdAndOwnerId(session.sessionId, owner)

        assertEquals(session.sessionId, result?.sessionId)
    }

    @Test
    fun `findBySessionIdAndOwnerId returns null for another user's session`() {
        val owner = persistUser()
        val stranger = persistUser()
        val session = persistSession(owner)

        assertNull(workoutSessionRepository.findBySessionIdAndOwnerId(session.sessionId, stranger))
    }

    @Test
    fun `findBySessionIdAndOwnerId returns null for a non-existent id`() {
        val owner = persistUser()

        assertNull(workoutSessionRepository.findBySessionIdAndOwnerId(999_999L, owner))
    }

    // ---- findByOwnerIdAndStatusIn ----

    @Test
    fun `findByOwnerIdAndStatusIn matches when the session's status is anywhere in the requested list`() {
        val owner = persistUser()
        val session = persistSession(owner, status = SessionStatus.PAUSED)

        val result = workoutSessionRepository.findByOwnerIdAndStatusIn(
            owner, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)
        )

        assertEquals(session.sessionId, result?.sessionId)
    }

    @Test
    fun `findByOwnerIdAndStatusIn returns null when no session matches any requested status`() {
        val owner = persistUser()
        persistSession(owner, status = SessionStatus.COMPLETED)

        val result = workoutSessionRepository.findByOwnerIdAndStatusIn(
            owner, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)
        )

        assertNull(result)
    }

    // ---- DB-level invariant: at most one ACTIVE/PAUSED session per owner ----

    @Test
    fun `the partial unique index rejects a second ACTIVE-or-PAUSED session for the same owner`() {
        // Goes through the repository (not TestEntityManager) so the real Spring Data proxy applies
        // its usual JDBC-to-DataAccessException translation, matching what production code actually
        // sees and what GlobalExceptionHandler's DataIntegrityViolationException handler catches.
        val owner = persistUser()
        persistSession(owner, status = SessionStatus.ACTIVE)

        assertThrows(DataIntegrityViolationException::class.java) {
            workoutSessionRepository.saveAndFlush(
                WorkoutSession(
                    ownerId = owner,
                    startedAt = OffsetDateTime.now(),
                    durationSeconds = 0,
                    status = SessionStatus.PAUSED
                )
            )
        }
    }

    @Test
    fun `the partial unique index does not restrict multiple COMPLETED sessions for the same owner`() {
        val owner = persistUser()
        persistSession(owner, status = SessionStatus.COMPLETED)

        // Must not throw - COMPLETED is outside the index's WHERE clause.
        persistSession(owner, status = SessionStatus.COMPLETED)

        assertEquals(2, workoutSessionRepository.findByOwnerId(owner).size)
    }

    // ---- findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc ----

    @Test
    fun `findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc returns at most 2, most recent first`() {
        val owner = persistUser()
        val dayId = persistWorkoutDay(owner)
        val now = OffsetDateTime.now()
        persistSession(owner, SessionStatus.COMPLETED, dayId, now.minusDays(3))
        persistSession(owner, SessionStatus.COMPLETED, dayId, now.minusDays(1))
        persistSession(owner, SessionStatus.COMPLETED, dayId, now.minusDays(2))

        val result = workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(
            owner, dayId, SessionStatus.COMPLETED
        )

        assertEquals(2, result.size)
        assertTrue(result[0].startedAt.isAfter(result[1].startedAt))
    }

    @Test
    fun `findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc excludes sessions for a different workout day`() {
        val owner = persistUser()
        val dayA = persistWorkoutDay(owner)
        val dayB = persistWorkoutDay(owner)
        persistSession(owner, SessionStatus.COMPLETED, dayB)

        val result = workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(
            owner, dayA, SessionStatus.COMPLETED
        )

        assertTrue(result.isEmpty())
    }

    // ---- findByOwnerIdAndStatusAndStartedAtAfter ----

    @Test
    fun `findByOwnerIdAndStatusAndStartedAtAfter excludes sessions started before the cutoff`() {
        val owner = persistUser()
        val now = OffsetDateTime.now()
        persistSession(owner, SessionStatus.COMPLETED, startedAt = now.minusDays(10))

        val result = workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(
            owner, SessionStatus.COMPLETED, now.minusDays(5)
        )

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByOwnerIdAndStatusAndStartedAtAfter includes sessions started after the cutoff`() {
        val owner = persistUser()
        val now = OffsetDateTime.now()
        val recent = persistSession(owner, SessionStatus.COMPLETED, startedAt = now.minusDays(1))

        val result = workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(
            owner, SessionStatus.COMPLETED, now.minusDays(5)
        )

        assertEquals(listOf(recent.sessionId), result.map { it.sessionId })
    }

    // ---- findTop3ByOwnerIdAndStatusOrderByStartedAtDesc ----

    @Test
    fun `findTop3ByOwnerIdAndStatusOrderByStartedAtDesc returns at most 3, most recent first`() {
        val owner = persistUser()
        val now = OffsetDateTime.now()
        for (i in 0..4) persistSession(owner, SessionStatus.COMPLETED, startedAt = now.minusDays(i.toLong()))

        val result = workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(owner, SessionStatus.COMPLETED)

        assertEquals(3, result.size)
        assertTrue(result.zipWithNext().all { (a, b) -> !a.startedAt.isBefore(b.startedAt) })
    }

    // ---- findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc ----

    @Test
    fun `findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc returns the most recent session across the given days`() {
        val owner = persistUser()
        val dayA = persistWorkoutDay(owner)
        val dayB = persistWorkoutDay(owner)
        val now = OffsetDateTime.now()
        persistSession(owner, SessionStatus.COMPLETED, dayA, now.minusDays(5))
        val mostRecent = persistSession(owner, SessionStatus.COMPLETED, dayB, now.minusDays(1))

        val result = workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
            owner, listOf(dayA, dayB), SessionStatus.COMPLETED
        )

        assertEquals(mostRecent.sessionId, result?.sessionId)
    }

    @Test
    fun `findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc returns null when no day in the list has a matching session`() {
        val owner = persistUser()
        val dayA = persistWorkoutDay(owner)

        val result = workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
            owner, listOf(dayA), SessionStatus.COMPLETED
        )

        assertNull(result)
    }
}
