package com.irontrail.api.session.dto

import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.irontrail.api.session.model.SessionStatus
import java.time.OffsetDateTime

data class WorkoutSessionRequest(
    val workoutDayId: Long? = null,
    val notes: String? = null
)

data class WorkoutSessionPatchRequest(
    val status: SessionStatus? = null,
    val durationSeconds: Long? = null,
    val totalVolumeKg: Double? = null,
    val completedSets: Int? = null,
    val totalSets: Int? = null,
    val notes: String? = null
)

data class WorkoutSessionResponse(
    val sessionId: Long,
    val workoutDayId: Long?,
    val splitNameSnapshot: String?,
    val workoutDayNameSnapshot: String?,
    val startedAt: OffsetDateTime,
    val durationSeconds: Long,
    val totalVolumeKg: Double?,
    val completedSets: Int?,
    val totalSets: Int?,
    val notes: String?,
    val status: SessionStatus
)

data class WorkoutSessionDetailResponse(
    @get:JsonUnwrapped val session: WorkoutSessionResponse,
    val sessionExercises: List<SessionExerciseResponse>
)