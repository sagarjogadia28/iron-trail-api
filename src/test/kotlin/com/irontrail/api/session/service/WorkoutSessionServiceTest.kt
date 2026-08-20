package com.irontrail.api.session.service

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.exercise.repository.ExerciseRepository
import com.irontrail.api.session.dto.SessionExercisePatchRequest
import com.irontrail.api.session.dto.SessionExerciseRequest
import com.irontrail.api.session.dto.SessionSetPatchRequest
import com.irontrail.api.session.dto.SessionSetRequest
import com.irontrail.api.session.dto.WorkoutSessionPatchRequest
import com.irontrail.api.session.dto.WorkoutSessionRequest
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.SessionExerciseRepository
import com.irontrail.api.session.repository.SessionSetRepository
import com.irontrail.api.session.repository.WorkoutSessionRepository
import com.irontrail.api.split.model.SetType
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.OffsetDateTime
import java.util.Optional

class WorkoutSessionServiceTest {

    private val workoutSessionRepository: WorkoutSessionRepository = mock()
    private val sessionExerciseRepository: SessionExerciseRepository = mock()
    private val sessionSetRepository: SessionSetRepository = mock()
    private val workoutDayRepository: WorkoutDayRepository = mock()
    private val splitRepository: SplitRepository = mock()
    private val exerciseRepository: ExerciseRepository = mock()
    private val ownershipResolver: SessionOwnershipResolver = mock()

    private val service = WorkoutSessionService(
        workoutSessionRepository,
        sessionExerciseRepository,
        sessionSetRepository,
        workoutDayRepository,
        splitRepository,
        exerciseRepository,
        ownershipResolver
    )

    // ---- fixtures ----

    private fun session(
        id: Long = 1L,
        ownerId: Long = 10L,
        status: SessionStatus = SessionStatus.ACTIVE,
        workoutDayId: Long? = null,
        splitNameSnapshot: String? = null,
        workoutDayNameSnapshot: String? = null,
        startedAt: OffsetDateTime = OffsetDateTime.now()
    ) = WorkoutSession(
        ownerId = ownerId,
        workoutDayId = workoutDayId,
        splitNameSnapshot = splitNameSnapshot,
        workoutDayNameSnapshot = workoutDayNameSnapshot,
        startedAt = startedAt,
        durationSeconds = 0,
        status = status
    ).apply { sessionId = id }

    private fun sessionExercise(
        id: Long = 1L,
        exerciseId: Long? = 100L,
        exerciseNameSnapshot: String = "Bench Press",
        inputTypeSnapshot: ExerciseInputType = ExerciseInputType.REPS,
        sortOrder: Int = 0,
        parentSession: WorkoutSession = session()
    ) = SessionExercise(
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        inputTypeSnapshot = inputTypeSnapshot,
        isRepRange = true,
        restDurationSeconds = 90,
        sortOrder = sortOrder
    ).apply {
        sessionExerciseId = id
        workoutSession = parentSession
    }

    private fun sessionSet(
        id: Long = 1L,
        sortOrder: Int = 0,
        parentExercise: SessionExercise = sessionExercise()
    ) = SessionSet(
        sortOrder = sortOrder,
        setType = SetType.NORMAL,
        isCompleted = false
    ).apply {
        sessionSetId = id
        sessionExercise = parentExercise
    }

    private fun exercise(
        id: Long = 100L,
        name: String = "Bench Press",
        inputType: ExerciseInputType = ExerciseInputType.REPS,
        ownerId: Long? = null
    ) = Exercise(
        wgerId = null,
        name = name,
        primaryMuscleGroup = MuscleGroup.CHEST,
        secondaryMuscleGroups = emptyList(),
        equipment = Equipment.BARBELL,
        inputType = inputType,
        description = null,
        imageUrl = null,
        ownerId = ownerId
    ).apply { exerciseId = id }

    private fun workoutDay(id: Long = 50L, splitId: Long = 5L, name: String = "Push Day", sortOrder: Int = 0) =
        WorkoutDay(splitId = splitId, name = name, sortOrder = sortOrder).apply { workoutDayId = id }

    private fun split(id: Long = 5L, ownerId: Long = 10L, name: String = "PPL") =
        Split(ownerId = ownerId, name = name).apply { splitId = id }

    // ---- findAll / splitName filter ----

    @Test
    fun `findAll returns every session for the owner sorted by startedAt descending when no splitName filter given`() {
        val older = session(id = 1L, startedAt = OffsetDateTime.now().minusDays(2))
        val newer = session(id = 2L, startedAt = OffsetDateTime.now())
        whenever(workoutSessionRepository.findByOwnerId(10L)).thenReturn(listOf(older, newer))

        val result = service.findAll(10L)

        assertEquals(listOf(2L, 1L), result.map { it.sessionId })
    }

    @Test
    fun `findAll filters to sessions whose splitNameSnapshot exactly matches the splitName param`() {
        val ppl = session(id = 1L, splitNameSnapshot = "PPL")
        val upperLower = session(id = 2L, splitNameSnapshot = "Upper Lower")
        whenever(workoutSessionRepository.findByOwnerId(10L)).thenReturn(listOf(ppl, upperLower))

        val result = service.findAll(10L, splitName = "PPL")

        assertEquals(listOf(1L), result.map { it.sessionId })
    }

    @Test
    fun `findAll splitName filter is case-sensitive, not a case-insensitive match`() {
        val ppl = session(id = 1L, splitNameSnapshot = "PPL")
        whenever(workoutSessionRepository.findByOwnerId(10L)).thenReturn(listOf(ppl))

        val result = service.findAll(10L, splitName = "ppl")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAll splitName filter excludes sessions with a null snapshot rather than treating null as a match`() {
        val noSplit = session(id = 1L, splitNameSnapshot = null)
        whenever(workoutSessionRepository.findByOwnerId(10L)).thenReturn(listOf(noSplit))

        val result = service.findAll(10L, splitName = "PPL")

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAll returns an empty list when the owner has no sessions at all`() {
        whenever(workoutSessionRepository.findByOwnerId(10L)).thenReturn(emptyList())

        assertTrue(service.findAll(10L).isEmpty())
    }

    // ---- findActive ----

    @Test
    fun `findActive returns null when the owner has no ACTIVE or PAUSED session`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(10L, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
            .thenReturn(null)

        assertNull(service.findActive(10L))
    }

    @Test
    fun `findActive returns the mapped session when one ACTIVE or PAUSED session exists`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(10L, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
            .thenReturn(session(id = 3L, status = SessionStatus.PAUSED))

        val result = service.findActive(10L)

        assertEquals(3L, result?.sessionId)
        assertEquals(SessionStatus.PAUSED, result?.status)
    }

    // Note: WorkoutSessionRepository.findByOwnerIdAndStatusIn is declared to return a single
    // nullable WorkoutSession, not a List, so "what if more than one active session existed"
    // cannot be exercised at this unit-test level - the repository signature itself only ever
    // hands the service zero or one candidate. That invariant is enforced at the DB layer
    // (partial unique index) and by create()/validateStatusTransition() below.

    // ---- findById ----

    @Test
    fun `findById returns the detail response with the nested session exercise tree`() {
        val s = session(id = 7L)
        whenever(ownershipResolver.getOwnedWorkoutSession(7L, 10L)).thenReturn(s)
        val se = sessionExercise(id = 1L, parentSession = s)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(s))).thenReturn(listOf(se))
        whenever(sessionSetRepository.findBySessionExerciseIn(listOf(se))).thenReturn(emptyList())

        val result = service.findById(7L, 10L)

        assertEquals(7L, result.session.sessionId)
        assertEquals(listOf(1L), result.sessionExercises.map { it.sessionExerciseId })
    }

    @Test
    fun `findById propagates NotFoundException from the ownership resolver when the session is not owned or missing`() {
        whenever(ownershipResolver.getOwnedWorkoutSession(7L, 10L)).thenThrow(NotFoundException("WorkoutSession", 7L))

        assertThrows(NotFoundException::class.java) { service.findById(7L, 10L) }
        verifyNoInteractions(sessionExerciseRepository)
    }

    // ---- create ----

    @Test
    fun `create rejects a new session when the owner already has an ACTIVE session`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(10L, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
            .thenReturn(session(status = SessionStatus.ACTIVE))

        assertThrows(ConflictException::class.java) {
            service.create(WorkoutSessionRequest(), 10L)
        }
        verify(workoutSessionRepository, never()).save(any())
    }

    @Test
    fun `create rejects a new session when the owner already has a PAUSED session`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(10L, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
            .thenReturn(session(status = SessionStatus.PAUSED))

        assertThrows(ConflictException::class.java) {
            service.create(WorkoutSessionRequest(), 10L)
        }
        verify(workoutSessionRepository, never()).save(any())
    }

    @Test
    fun `create allows a new session when the owner's only existing session is COMPLETED`() {
        // The repository query itself is scoped to ACTIVE/PAUSED, so a COMPLETED-only owner
        // correctly yields null here - this stubs that real-world repository behavior.
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(10L, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)))
            .thenReturn(null)
        whenever(workoutSessionRepository.save(any())).thenAnswer { it.arguments[0] as WorkoutSession }

        val result = service.create(WorkoutSessionRequest(), 10L)

        assertEquals(SessionStatus.ACTIVE, result.session.status)
    }

    @Test
    fun `create with no workoutDayId leaves both snapshot fields null and never queries workoutDay or split repositories`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(any(), any())).thenReturn(null)
        val captor = argumentCaptor<WorkoutSession>()
        whenever(workoutSessionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as WorkoutSession }

        service.create(WorkoutSessionRequest(workoutDayId = null), 10L)

        assertNull(captor.firstValue.splitNameSnapshot)
        assertNull(captor.firstValue.workoutDayNameSnapshot)
        verifyNoInteractions(workoutDayRepository)
        verifyNoInteractions(splitRepository)
    }

    @Test
    fun `create with a workoutDayId resolves splitNameSnapshot and workoutDayNameSnapshot server-side from the live entities`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(any(), any())).thenReturn(null)
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(50L, 10L)).thenReturn(workoutDay(id = 50L, splitId = 5L, name = "Push Day"))
        whenever(splitRepository.findById(5L)).thenReturn(Optional.of(split(id = 5L, name = "PPL")))
        val captor = argumentCaptor<WorkoutSession>()
        whenever(workoutSessionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as WorkoutSession }

        service.create(WorkoutSessionRequest(workoutDayId = 50L), 10L)

        assertEquals("PPL", captor.firstValue.splitNameSnapshot)
        assertEquals("Push Day", captor.firstValue.workoutDayNameSnapshot)
        assertEquals(50L, captor.firstValue.workoutDayId)
    }

    @Test
    fun `create throws NotFoundException when the given workoutDayId is not owned by the caller`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(any(), any())).thenReturn(null)
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(50L, 10L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.create(WorkoutSessionRequest(workoutDayId = 50L), 10L)
        }
        verify(workoutSessionRepository, never()).save(any())
    }

    @Test
    fun `create throws NotFoundException when the workoutDay's parent split can't be found`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(any(), any())).thenReturn(null)
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(50L, 10L)).thenReturn(workoutDay(id = 50L, splitId = 5L))
        whenever(splitRepository.findById(5L)).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            service.create(WorkoutSessionRequest(workoutDayId = 50L), 10L)
        }
        verify(workoutSessionRepository, never()).save(any())
    }

    @Test
    fun `create always starts a new session as ACTIVE with zero duration regardless of request contents`() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusIn(any(), any())).thenReturn(null)
        val captor = argumentCaptor<WorkoutSession>()
        whenever(workoutSessionRepository.save(captor.capture())).thenAnswer { it.arguments[0] as WorkoutSession }

        service.create(WorkoutSessionRequest(notes = "leg day"), 10L)

        assertEquals(SessionStatus.ACTIVE, captor.firstValue.status)
        assertEquals(0L, captor.firstValue.durationSeconds)
        assertEquals("leg day", captor.firstValue.notes)
    }

    // ---- update / status transitions ----

    @Test
    fun `update rejects transitioning a COMPLETED session to ACTIVE - COMPLETED is terminal`() {
        val s = session(id = 1L, status = SessionStatus.COMPLETED)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)

        assertThrows(ConflictException::class.java) {
            service.update(1L, WorkoutSessionPatchRequest(status = SessionStatus.ACTIVE), 10L)
        }
        assertEquals(SessionStatus.COMPLETED, s.status)
    }

    @Test
    fun `update rejects transitioning a COMPLETED session to PAUSED - COMPLETED is terminal`() {
        val s = session(id = 1L, status = SessionStatus.COMPLETED)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)

        assertThrows(ConflictException::class.java) {
            service.update(1L, WorkoutSessionPatchRequest(status = SessionStatus.PAUSED), 10L)
        }
    }

    @Test
    fun `update allows re-sending COMPLETED on an already-COMPLETED session without throwing`() {
        val s = session(id = 1L, status = SessionStatus.COMPLETED)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(s))).thenReturn(emptyList())
        whenever(sessionSetRepository.findBySessionExerciseIn(any())).thenReturn(emptyList())

        val result = service.update(1L, WorkoutSessionPatchRequest(status = SessionStatus.COMPLETED), 10L)

        assertEquals(SessionStatus.COMPLETED, result.session.status)
    }

    @Test
    fun `update allows an ACTIVE session to move to PAUSED`() {
        val s = session(id = 1L, status = SessionStatus.ACTIVE)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(s))).thenReturn(emptyList())
        whenever(sessionSetRepository.findBySessionExerciseIn(any())).thenReturn(emptyList())

        val result = service.update(1L, WorkoutSessionPatchRequest(status = SessionStatus.PAUSED), 10L)

        assertEquals(SessionStatus.PAUSED, result.session.status)
    }

    @Test
    fun `update allows an ACTIVE session to move to COMPLETED`() {
        val s = session(id = 1L, status = SessionStatus.ACTIVE)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(s))).thenReturn(emptyList())
        whenever(sessionSetRepository.findBySessionExerciseIn(any())).thenReturn(emptyList())

        val result = service.update(1L, WorkoutSessionPatchRequest(status = SessionStatus.COMPLETED), 10L)

        assertEquals(SessionStatus.COMPLETED, result.session.status)
    }

    @Test
    fun `update rejects patching notes on an already-COMPLETED session`() {
        val s = session(id = 1L, status = SessionStatus.COMPLETED).apply { notes = "original" }
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)

        assertThrows(ConflictException::class.java) {
            service.update(1L, WorkoutSessionPatchRequest(notes = "great session"), 10L)
        }
        assertEquals("original", s.notes)
    }

    @Test
    fun `update only applies fields present in the patch request, leaving omitted fields unchanged`() {
        val s = session(id = 1L, status = SessionStatus.ACTIVE).apply {
            durationSeconds = 500
            totalVolumeKg = 1000.0
            completedSets = 3
            totalSets = 5
            notes = "original"
        }
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(s))).thenReturn(emptyList())
        whenever(sessionSetRepository.findBySessionExerciseIn(any())).thenReturn(emptyList())

        val result = service.update(1L, WorkoutSessionPatchRequest(completedSets = 4), 10L)

        assertEquals(500L, result.session.durationSeconds)
        assertEquals(1000.0, result.session.totalVolumeKg)
        assertEquals(4, result.session.completedSets)
        assertEquals(5, result.session.totalSets)
        assertEquals("original", result.session.notes)
    }

    @Test
    fun `update propagates NotFoundException from the ownership resolver and never touches the tree repositories`() {
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenThrow(NotFoundException("WorkoutSession", 1L))

        assertThrows(NotFoundException::class.java) {
            service.update(1L, WorkoutSessionPatchRequest(notes = "x"), 10L)
        }
        verifyNoInteractions(sessionExerciseRepository)
    }

    // ---- delete ----

    @Test
    fun `delete fetches the owned session and deletes exactly that entity`() {
        val s = session(id = 1L)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)

        service.delete(1L, 10L)

        verify(workoutSessionRepository).delete(s)
    }

    @Test
    fun `delete does not call repository delete when the session is not owned or missing`() {
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenThrow(NotFoundException("WorkoutSession", 1L))

        assertThrows(NotFoundException::class.java) { service.delete(1L, 10L) }
        verify(workoutSessionRepository, never()).delete(any())
    }

    // ---- createSessionExercise ----

    @Test
    fun `createSessionExercise derives exerciseNameSnapshot and inputTypeSnapshot from the live exercise, never from client input`() {
        val s = session(id = 1L)
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(s)
        whenever(exerciseRepository.findVisibleById(100L, 10L))
            .thenReturn(exercise(id = 100L, name = "Back Squat", inputType = ExerciseInputType.REPS))
        val captor = argumentCaptor<SessionExercise>()
        whenever(sessionExerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as SessionExercise }

        // SessionExerciseRequest itself has no exerciseNameSnapshot/inputTypeSnapshot field at all -
        // there is no way for a caller to supply one, confirming server-side derivation structurally.
        val result = service.createSessionExercise(
            1L,
            SessionExerciseRequest(exerciseId = 100L, isRepRange = true, restDurationSeconds = 90, sortOrder = 0),
            10L
        )

        assertEquals("Back Squat", captor.firstValue.exerciseNameSnapshot)
        assertEquals(ExerciseInputType.REPS, captor.firstValue.inputTypeSnapshot)
        assertEquals("Back Squat", result.exerciseNameSnapshot)
    }

    @Test
    fun `createSessionExercise throws NotFoundException when the exercise is not visible to the caller`() {
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenReturn(session(id = 1L))
        whenever(exerciseRepository.findVisibleById(100L, 10L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            service.createSessionExercise(
                1L,
                SessionExerciseRequest(exerciseId = 100L, isRepRange = true, restDurationSeconds = 90, sortOrder = 0),
                10L
            )
        }
        verify(sessionExerciseRepository, never()).save(any())
    }

    @Test
    fun `createSessionExercise never queries the exercise repository when the parent session is not owned`() {
        whenever(ownershipResolver.getOwnedWorkoutSession(1L, 10L)).thenThrow(NotFoundException("WorkoutSession", 1L))

        assertThrows(NotFoundException::class.java) {
            service.createSessionExercise(
                1L,
                SessionExerciseRequest(exerciseId = 100L, isRepRange = true, restDurationSeconds = 90, sortOrder = 0),
                10L
            )
        }
        verifyNoInteractions(exerciseRepository)
    }

    // ---- updateSessionExercise ----

    @Test
    fun `updateSessionExercise only applies fields present in the patch, leaving others unchanged`() {
        val se = sessionExercise(id = 1L, sortOrder = 0).apply {
            restDurationSeconds = 90
            isRepRange = true
            notes = "original"
        }
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)

        val result = service.updateSessionExercise(1L, SessionExercisePatchRequest(sortOrder = 2), 10L)

        assertEquals(2, result.sortOrder)
        assertEquals(90, result.restDurationSeconds)
        assertEquals(true, result.isRepRange)
        assertEquals("original", result.notes)
    }

    @Test
    fun `updateSessionExercise returns sets read from the entity's own sets relation`() {
        val se = sessionExercise(id = 1L)
        val set1 = sessionSet(id = 1L, sortOrder = 1, parentExercise = se)
        val set0 = sessionSet(id = 2L, sortOrder = 0, parentExercise = se)
        se.sets.add(set1)
        se.sets.add(set0)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)

        val result = service.updateSessionExercise(1L, SessionExercisePatchRequest(notes = "n"), 10L)

        assertEquals(listOf(2L, 1L), result.sets.map { it.sessionSetId })
        verifyNoInteractions(sessionSetRepository)
    }

    // ---- deleteSessionExercise ----

    @Test
    fun `deleteSessionExercise fetches the owned exercise and deletes exactly that entity`() {
        val se = sessionExercise(id = 1L)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)

        service.deleteSessionExercise(1L, 10L)

        verify(sessionExerciseRepository).delete(se)
    }

    // ---- findPreviousPerformance ----

    @Test
    fun `findPreviousPerformance returns empty list when the session has no workoutDayId`() {
        val s = session(id = 1L, workoutDayId = null)
        val se = sessionExercise(id = 1L, parentSession = s)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)

        val result = service.findPreviousPerformance(1L, 10L)

        assertTrue(result.isEmpty())
        verifyNoInteractions(workoutSessionRepository)
    }

    @Test
    fun `findPreviousPerformance returns empty list when the session exercise has no exerciseId`() {
        val s = session(id = 1L, workoutDayId = 50L)
        val se = sessionExercise(id = 1L, exerciseId = null, parentSession = s)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)

        val result = service.findPreviousPerformance(1L, 10L)

        assertTrue(result.isEmpty())
        verifyNoInteractions(workoutSessionRepository)
    }

    @Test
    fun `findPreviousPerformance returns empty list when no completed session exists for that day at all`() {
        val s = session(id = 1L, workoutDayId = 50L)
        val se = sessionExercise(id = 1L, parentSession = s)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(10L, 50L, SessionStatus.COMPLETED))
            .thenReturn(emptyList())

        assertTrue(service.findPreviousPerformance(1L, 10L).isEmpty())
    }

    @Test
    fun `findPreviousPerformance excludes the current session even when it is the only completed session that day`() {
        val s = session(id = 1L, workoutDayId = 50L, status = SessionStatus.COMPLETED)
        val se = sessionExercise(id = 1L, parentSession = s)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(10L, 50L, SessionStatus.COMPLETED))
            .thenReturn(listOf(s))

        val result = service.findPreviousPerformance(1L, 10L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findPreviousPerformance picks the other session when the current session is the more recent of the top-2 results`() {
        val current = session(id = 1L, workoutDayId = 50L, status = SessionStatus.COMPLETED)
        val other = session(id = 2L, workoutDayId = 50L, status = SessionStatus.COMPLETED)
        val se = sessionExercise(id = 1L, exerciseId = 100L, parentSession = current)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        // current listed first (most recent), other second - the query is Top2-bounded precisely
        // so that after self-exclusion a real candidate is still available.
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(10L, 50L, SessionStatus.COMPLETED))
            .thenReturn(listOf(current, other))

        val otherExercise = sessionExercise(id = 2L, exerciseId = 100L, parentSession = other)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(other))).thenReturn(listOf(otherExercise))
        val otherSet = sessionSet(id = 5L, sortOrder = 0, parentExercise = otherExercise)
        whenever(sessionSetRepository.findBySessionExerciseIn(listOf(otherExercise))).thenReturn(listOf(otherSet))

        val result = service.findPreviousPerformance(1L, 10L)

        assertEquals(listOf(5L), result.map { it.sessionSetId })
    }

    @Test
    fun `findPreviousPerformance does not match a session-exercise for a different exercise on the same matched day`() {
        val current = session(id = 1L, workoutDayId = 50L, status = SessionStatus.ACTIVE)
        val other = session(id = 2L, workoutDayId = 50L, status = SessionStatus.COMPLETED)
        val se = sessionExercise(id = 1L, exerciseId = 100L, parentSession = current)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(10L, 50L, SessionStatus.COMPLETED))
            .thenReturn(listOf(other))

        // "other" session's exercise on that day is a different exercise (e.g. Squat, id 999) -
        // must not be treated as a match for exerciseId 100.
        val differentExercise = sessionExercise(id = 3L, exerciseId = 999L, parentSession = other)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(other))).thenReturn(listOf(differentExercise))

        val result = service.findPreviousPerformance(1L, 10L)

        assertTrue(result.isEmpty())
        verifyNoInteractions(sessionSetRepository)
    }

    @Test
    fun `findPreviousPerformance is scoped to the correct owner and workoutDayId via repository params`() {
        val s = session(id = 1L, workoutDayId = 50L)
        val se = sessionExercise(id = 1L, exerciseId = 100L, parentSession = s)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(any(), any(), any()))
            .thenReturn(emptyList())

        service.findPreviousPerformance(1L, 10L)

        verify(workoutSessionRepository).findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(
            eq(10L), eq(50L), eq(SessionStatus.COMPLETED)
        )
    }

    @Test
    fun `findPreviousPerformance returns the previous session's sets sorted by sortOrder`() {
        val current = session(id = 1L, workoutDayId = 50L, status = SessionStatus.ACTIVE)
        val other = session(id = 2L, workoutDayId = 50L, status = SessionStatus.COMPLETED)
        val se = sessionExercise(id = 1L, exerciseId = 100L, parentSession = current)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        whenever(workoutSessionRepository.findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(10L, 50L, SessionStatus.COMPLETED))
            .thenReturn(listOf(other))
        val otherExercise = sessionExercise(id = 9L, exerciseId = 100L, parentSession = other)
        whenever(sessionExerciseRepository.findByWorkoutSessionIn(listOf(other))).thenReturn(listOf(otherExercise))
        val setB = sessionSet(id = 2L, sortOrder = 1, parentExercise = otherExercise)
        val setA = sessionSet(id = 1L, sortOrder = 0, parentExercise = otherExercise)
        whenever(sessionSetRepository.findBySessionExerciseIn(listOf(otherExercise))).thenReturn(listOf(setB, setA))

        val result = service.findPreviousPerformance(1L, 10L)

        assertEquals(listOf(1L, 2L), result.map { it.sessionSetId })
    }

    // ---- createSessionSet ----

    @Test
    fun `createSessionSet always starts a set as not completed with no logged values, regardless of request`() {
        val se = sessionExercise(id = 1L)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        val captor = argumentCaptor<SessionSet>()
        whenever(sessionSetRepository.save(captor.capture())).thenAnswer { it.arguments[0] as SessionSet }

        val result = service.createSessionSet(1L, SessionSetRequest(sortOrder = 0, setType = SetType.NORMAL, targetReps = 8), 10L)

        assertEquals(false, captor.firstValue.isCompleted)
        assertNull(captor.firstValue.reps)
        assertNull(captor.firstValue.weightKg)
        assertNull(captor.firstValue.durationSeconds)
        assertEquals(false, result.isCompleted)
    }

    @Test
    fun `createSessionSet copies planned fields from the request and links to the owned parent exercise`() {
        val se = sessionExercise(id = 1L)
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenReturn(se)
        val captor = argumentCaptor<SessionSet>()
        whenever(sessionSetRepository.save(captor.capture())).thenAnswer { it.arguments[0] as SessionSet }

        service.createSessionSet(
            1L,
            SessionSetRequest(sortOrder = 2, setType = SetType.WARMUP, targetReps = 5, targetRepsMax = 8, targetDurationSeconds = null),
            10L
        )

        assertEquals(2, captor.firstValue.sortOrder)
        assertEquals(SetType.WARMUP, captor.firstValue.setType)
        assertEquals(5, captor.firstValue.targetReps)
        assertEquals(8, captor.firstValue.targetRepsMax)
        assertSame(se, captor.firstValue.sessionExercise)
    }

    @Test
    fun `createSessionSet throws NotFoundException when the parent session exercise is not owned`() {
        whenever(ownershipResolver.getOwnedSessionExercise(1L, 10L)).thenThrow(NotFoundException("SessionExercise", 1L))

        assertThrows(NotFoundException::class.java) {
            service.createSessionSet(1L, SessionSetRequest(sortOrder = 0, setType = SetType.NORMAL), 10L)
        }
        verify(sessionSetRepository, never()).save(any())
    }

    // ---- updateSessionSet ----

    @Test
    fun `updateSessionSet applies an explicit isCompleted=false, distinguishing it from an omitted field`() {
        val set = sessionSet(id = 1L).apply { isCompleted = true }
        whenever(ownershipResolver.getOwnedSessionSet(1L, 10L)).thenReturn(set)

        val result = service.updateSessionSet(1L, SessionSetPatchRequest(isCompleted = false), 10L)

        assertEquals(false, result.isCompleted)
    }

    @Test
    fun `updateSessionSet leaves isCompleted unchanged when the patch omits it`() {
        val set = sessionSet(id = 1L).apply { isCompleted = true }
        whenever(ownershipResolver.getOwnedSessionSet(1L, 10L)).thenReturn(set)

        val result = service.updateSessionSet(1L, SessionSetPatchRequest(reps = 10), 10L)

        assertEquals(true, result.isCompleted)
        assertEquals(10, result.reps)
    }

    @Test
    fun `updateSessionSet logs actual reps, weight and duration via patch`() {
        val set = sessionSet(id = 1L)
        whenever(ownershipResolver.getOwnedSessionSet(1L, 10L)).thenReturn(set)

        val result = service.updateSessionSet(
            1L,
            SessionSetPatchRequest(reps = 10, weightKg = 60.0, durationSeconds = null, isCompleted = true),
            10L
        )

        assertEquals(10, result.reps)
        assertEquals(60.0, result.weightKg)
        assertEquals(true, result.isCompleted)
    }

    // ---- deleteSessionSet ----

    @Test
    fun `deleteSessionSet fetches the owned set and deletes exactly that entity`() {
        val set = sessionSet(id = 1L)
        whenever(ownershipResolver.getOwnedSessionSet(1L, 10L)).thenReturn(set)

        service.deleteSessionSet(1L, 10L)

        verify(sessionSetRepository).delete(set)
    }

    @Test
    fun `deleteSessionSet does not call repository delete when the set is not owned or missing`() {
        whenever(ownershipResolver.getOwnedSessionSet(1L, 10L)).thenThrow(NotFoundException("SessionSet", 1L))

        assertThrows(NotFoundException::class.java) { service.deleteSessionSet(1L, 10L) }
        verify(sessionSetRepository, never()).delete(any())
    }
}
