package com.irontrail.api.split.dto

data class TemplateExerciseRequest(
    val exerciseId: Long,
    val sortOrder: Int,
    val restDurationSeconds: Int = 90,
    val isRepRange: Boolean = true,
    val notes: String? = null
)

data class TemplateExerciseResponse(
    val templateExerciseId: Long,
    val exerciseId: Long,
    val sortOrder: Int,
    val restDurationSeconds: Int,
    val isRepRange: Boolean,
    val notes: String?,
    val templateSets: List<TemplateSetResponse>
)