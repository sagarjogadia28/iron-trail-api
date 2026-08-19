package com.irontrail.api.session.service

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.repository.ExerciseRepository
import com.irontrail.api.session.dto.SessionExercisePatchRequest
import com.irontrail.api.session.dto.SessionExerciseRequest
import com.irontrail.api.session.dto.SessionExerciseResponse
import com.irontrail.api.session.dto.SessionSetPatchRequest
import com.irontrail.api.session.dto.SessionSetRequest
import com.irontrail.api.session.dto.SessionSetResponse
import com.irontrail.api.session.dto.WorkoutSessionDetailResponse
import com.irontrail.api.session.dto.WorkoutSessionPatchRequest
import com.irontrail.api.session.dto.WorkoutSessionRequest
import com.irontrail.api.session.dto.WorkoutSessionResponse
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.SessionExerciseRepository
import com.irontrail.api.session.repository.SessionSetRepository
import com.irontrail.api.session.repository.WorkoutSessionRepository
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
@Transactional
class WorkoutSessionService(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val sessionExerciseRepository: SessionExerciseRepository,
    private val sessionSetRepository: SessionSetRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val splitRepository: SplitRepository,
    private val exerciseRepository: ExerciseRepository,
    private val ownershipResolver: SessionOwnershipResolver
) {
    private val activeSessionStatuses = listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED)


    fun findAll(userId: Long, splitName: String? = null): List<WorkoutSessionResponse> =
        workoutSessionRepository.findByOwnerId(userId)
            .filter { splitName == null || it.splitNameSnapshot == splitName }
            .sortedByDescending { it.startedAt }
            .map { it.toResponse() }

    fun findActive(userId: Long): WorkoutSessionResponse? =
        workoutSessionRepository.findByOwnerIdAndStatusIn(userId, listOf(SessionStatus.ACTIVE, SessionStatus.PAUSED))
            ?.toResponse()

    fun findById(sessionId: Long, userId: Long): WorkoutSessionDetailResponse {
        val session = ownershipResolver.getOwnedWorkoutSession(sessionId, userId)
        val sessionExercises = buildSessionExerciseTree(listOf(session)).getValue(session.sessionId)
        return session.toDetailResponse(sessionExercises)
    }

    fun create(request: WorkoutSessionRequest, userId: Long): WorkoutSessionDetailResponse {
        if (workoutSessionRepository.findByOwnerIdAndStatusIn(userId, activeSessionStatuses) != null) {
            throw ConflictException("An active session already exists")
        }

        var splitNameSnapshot: String? = null
        var workoutDayNameSnapshot: String? = null
        if (request.workoutDayId != null) {
            val workoutDay = workoutDayRepository.findOwnedByWorkoutDayId(request.workoutDayId, userId)
                ?: throw NotFoundException("WorkoutDay", request.workoutDayId)
            val split = splitRepository.findById(workoutDay.splitId).orElseThrow { NotFoundException("Split", workoutDay.splitId) }
            workoutDayNameSnapshot = workoutDay.name
            splitNameSnapshot = split.name
        }

        val saved = workoutSessionRepository.save(
            WorkoutSession(
                ownerId = userId,
                workoutDayId = request.workoutDayId,
                splitNameSnapshot = splitNameSnapshot,
                workoutDayNameSnapshot = workoutDayNameSnapshot,
                startedAt = OffsetDateTime.now(),
                durationSeconds = 0,
                notes = request.notes,
                status = SessionStatus.ACTIVE
            )
        )
        return saved.toDetailResponse(emptyList())
    }

    fun update(sessionId: Long, request: WorkoutSessionPatchRequest, userId: Long): WorkoutSessionDetailResponse {
        val session = ownershipResolver.getOwnedWorkoutSession(sessionId, userId)
        request.status?.let { newStatus ->
            validateStatusTransition(session, newStatus, userId)
            session.status = newStatus
        }
        request.durationSeconds?.let { session.durationSeconds = it }
        request.totalVolumeKg?.let { session.totalVolumeKg = it }
        request.completedSets?.let { session.completedSets = it }
        request.totalSets?.let { session.totalSets = it }
        request.notes?.let { session.notes = it }
        val sessionExercises = buildSessionExerciseTree(listOf(session)).getValue(session.sessionId)
        return session.toDetailResponse(sessionExercises)
    }

    fun delete(sessionId: Long, userId: Long) {
        val session = ownershipResolver.getOwnedWorkoutSession(sessionId, userId)
        workoutSessionRepository.delete(session)
    }

    //SessionExercise
    fun createSessionExercise(sessionId: Long, request: SessionExerciseRequest, userId: Long): SessionExerciseResponse {
        val session = ownershipResolver.getOwnedWorkoutSession(sessionId, userId)
        val exercise = exerciseRepository.findVisibleById(request.exerciseId, userId)
            ?: throw NotFoundException("Exercise", request.exerciseId)

        val saved = sessionExerciseRepository.save(
            SessionExercise(
                exerciseId = exercise.exerciseId,
                exerciseNameSnapshot = exercise.name,
                inputTypeSnapshot = exercise.inputType,
                isRepRange = request.isRepRange,
                restDurationSeconds = request.restDurationSeconds,
                sortOrder = request.sortOrder,
                notes = request.notes
            ).apply { workoutSession = session }
        )
        return saved.toResponse(emptyList())
    }

    fun updateSessionExercise(sessionExerciseId: Long, request: SessionExercisePatchRequest, userId: Long): SessionExerciseResponse {
        val sessionExercise = ownershipResolver.getOwnedSessionExercise(sessionExerciseId, userId)
        request.sortOrder?.let { sessionExercise.sortOrder = it }
        request.restDurationSeconds?.let { sessionExercise.restDurationSeconds = it }
        request.isRepRange?.let { sessionExercise.isRepRange = it }
        request.notes?.let { sessionExercise.notes = it }
        return sessionExercise.toResponse(buildSessionSetResponses(sessionExercise))
    }

    fun deleteSessionExercise(sessionExerciseId: Long, userId: Long) {
        val sessionExercise = ownershipResolver.getOwnedSessionExercise(sessionExerciseId, userId)
        sessionExerciseRepository.delete(sessionExercise)
    }

    fun findPreviousPerformance(sessionExerciseId: Long, userId: Long): List<SessionSetResponse> {
        val sessionExercise = ownershipResolver.getOwnedSessionExercise(sessionExerciseId, userId)
        val workoutDayId = sessionExercise.workoutSession.workoutDayId ?: return emptyList()
        val exerciseId = sessionExercise.exerciseId ?: return emptyList()

        val previousSession = workoutSessionRepository
            .findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(userId, workoutDayId, SessionStatus.COMPLETED)
            .firstOrNull { it.sessionId != sessionExercise.workoutSession.sessionId }
            ?: return emptyList()

        val previousExercise = sessionExerciseRepository.findByWorkoutSessionIn(listOf(previousSession))
            .firstOrNull { it.exerciseId == exerciseId }
            ?: return emptyList()

        return sessionSetRepository.findBySessionExerciseIn(listOf(previousExercise))
            .sortedBy { it.sortOrder }
            .map { it.toResponse() }
    }

    //SessionSet
    fun createSessionSet(sessionExerciseId: Long, request: SessionSetRequest, userId: Long): SessionSetResponse {
        val parentExercise = ownershipResolver.getOwnedSessionExercise(sessionExerciseId, userId)
        val saved = sessionSetRepository.save(
            SessionSet(
                sortOrder = request.sortOrder,
                setType = request.setType,
                targetReps = request.targetReps,
                targetRepsMax = request.targetRepsMax,
                targetDurationSeconds = request.targetDurationSeconds,
                isCompleted = false
            ).apply { sessionExercise = parentExercise }
        )
        return saved.toResponse()
    }

    fun updateSessionSet(sessionSetId: Long, request: SessionSetPatchRequest, userId: Long): SessionSetResponse {
        val sessionSet = ownershipResolver.getOwnedSessionSet(sessionSetId, userId)
        request.sortOrder?.let { sessionSet.sortOrder = it }
        request.setType?.let { sessionSet.setType = it }
        request.reps?.let { sessionSet.reps = it }
        request.weightKg?.let { sessionSet.weightKg = it }
        request.durationSeconds?.let { sessionSet.durationSeconds = it }
        request.isCompleted?.let { sessionSet.isCompleted = it }
        return sessionSet.toResponse()
    }

    fun deleteSessionSet(sessionSetId: Long, userId: Long) {
        val sessionSet = ownershipResolver.getOwnedSessionSet(sessionSetId, userId)
        sessionSetRepository.delete(sessionSet)
    }

    private fun validateStatusTransition(session: WorkoutSession, newStatus: SessionStatus, userId: Long) {
        if (session.status == SessionStatus.COMPLETED && newStatus != SessionStatus.COMPLETED) {
            throw ConflictException("Cannot change the status of a completed session")
        }
        if (newStatus in activeSessionStatuses && session.status !in activeSessionStatuses) {
            val existingActive = workoutSessionRepository.findByOwnerIdAndStatusIn(userId, activeSessionStatuses)
            if (existingActive != null && existingActive.sessionId != session.sessionId) {
                throw ConflictException("An active session already exists")
            }
        }
    }

    private fun buildSessionExerciseTree(sessions: List<WorkoutSession>): Map<Long, List<SessionExerciseResponse>> {
        if (sessions.isEmpty()) return emptyMap()
        val sessionExercises = sessionExerciseRepository.findByWorkoutSessionIn(sessions)
        val sessionSets = sessionSetRepository.findBySessionExerciseIn(sessionExercises)
        val setsByExerciseId = sessionSets.groupBy { it.sessionExercise.sessionExerciseId }
        val exercisesBySessionId = sessionExercises.groupBy { it.workoutSession.sessionId }

        return sessions.associate { session ->
            session.sessionId to exercisesBySessionId[session.sessionId].orEmpty().sortedBy { it.sortOrder }.map { se ->
                val sets = setsByExerciseId[se.sessionExerciseId].orEmpty().sortedBy { it.sortOrder }.map { it.toResponse() }
                se.toResponse(sets)
            }
        }
    }

    private fun buildSessionSetResponses(sessionExercise: SessionExercise): List<SessionSetResponse> =
        sessionExercise.sets.sortedBy { it.sortOrder }.map { it.toResponse() }

    private fun WorkoutSession.toResponse() = WorkoutSessionResponse(
        sessionId = sessionId,
        workoutDayId = workoutDayId,
        splitNameSnapshot = splitNameSnapshot,
        workoutDayNameSnapshot = workoutDayNameSnapshot,
        startedAt = startedAt,
        durationSeconds = durationSeconds,
        totalVolumeKg = totalVolumeKg,
        completedSets = completedSets,
        totalSets = totalSets,
        notes = notes,
        status = status
    )

    private fun WorkoutSession.toDetailResponse(sessionExercises: List<SessionExerciseResponse>) = WorkoutSessionDetailResponse(
        session = toResponse(),
        sessionExercises = sessionExercises
    )

    private fun SessionExercise.toResponse(sets: List<SessionSetResponse>) = SessionExerciseResponse(
        sessionExerciseId = sessionExerciseId,
        exerciseId = exerciseId,
        exerciseNameSnapshot = exerciseNameSnapshot,
        inputTypeSnapshot = inputTypeSnapshot,
        isRepRange = isRepRange,
        restDurationSeconds = restDurationSeconds,
        sortOrder = sortOrder,
        notes = notes,
        sets = sets
    )

    private fun SessionSet.toResponse() = SessionSetResponse(
        sessionSetId = sessionSetId,
        sortOrder = sortOrder,
        setType = setType,
        targetReps = targetReps,
        targetRepsMax = targetRepsMax,
        targetDurationSeconds = targetDurationSeconds,
        reps = reps,
        weightKg = weightKg,
        durationSeconds = durationSeconds,
        isCompleted = isCompleted
    )
}
