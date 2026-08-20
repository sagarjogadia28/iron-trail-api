package com.irontrail.api.split.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class SplitRequest(
    @field:NotBlank
    val name: String,
)

data class SplitPatchRequest(
    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val name: String? = null,
)

data class SplitResponse(
    val splitId: Long,
    val name: String,
    val workoutDayCount: Int,
    val exerciseCount: Int,
)

data class SplitDetailResponse(
    val splitId: Long,
    val name: String,
    val workoutDays: List<WorkoutDayResponse>,
)
