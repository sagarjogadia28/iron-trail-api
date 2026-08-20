package com.irontrail.api.session.dto

import com.irontrail.api.exercise.model.ExerciseInputType

data class SessionExerciseRequest(
    val exerciseId: Long,
    val isRepRange: Boolean,
    val restDurationSeconds: Int,
    val sortOrder: Int,
    val notes: String? = null,
)

data class SessionExercisePatchRequest(
    val sortOrder: Int? = null,
    val restDurationSeconds: Int? = null,
    val isRepRange: Boolean? = null,
    val notes: String? = null,
)

data class SessionExerciseResponse(
    val sessionExerciseId: Long,
    val exerciseId: Long?,
    val exerciseNameSnapshot: String,
    val inputTypeSnapshot: ExerciseInputType,
    val isRepRange: Boolean,
    val restDurationSeconds: Int,
    val sortOrder: Int,
    val notes: String?,
    val sets: List<SessionSetResponse>,
)
