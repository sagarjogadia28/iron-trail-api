package com.irontrail.api.split.dto

import com.irontrail.api.split.model.SetType

data class TemplateSetResponse(
    val templateSetId: Long,
    val sortOrder: Int,
    val targetReps: Int?,
    val targetRepsMax: Int?,
    val targetDurationSeconds: Int?,
    val setType: SetType
)