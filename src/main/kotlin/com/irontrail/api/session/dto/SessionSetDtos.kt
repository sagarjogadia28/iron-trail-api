package com.irontrail.api.session.dto

import com.irontrail.api.split.model.SetType

data class SessionSetRequest(
    val sortOrder: Int,
    val setType: SetType,
    val targetReps: Int? = null,
    val targetRepsMax: Int? = null,
    val targetDurationSeconds: Int? = null
)

data class SessionSetPatchRequest(
    val sortOrder: Int? = null,
    val setType: SetType? = null,
    val reps: Int? = null,
    val weightKg: Double? = null,
    val durationSeconds: Int? = null,
    val isCompleted: Boolean? = null
)

data class SessionSetResponse(
    val sessionSetId: Long,
    val sortOrder: Int,
    val setType: SetType,
    val targetReps: Int?,
    val targetRepsMax: Int?,
    val targetDurationSeconds: Int?,
    val reps: Int?,
    val weightKg: Double?,
    val durationSeconds: Int?,
    val isCompleted: Boolean
)