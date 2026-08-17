package com.irontrail.api.split.dto

import jakarta.validation.constraints.NotBlank

data class SplitRequest(
    @field:NotBlank
    val name: String
)

data class SplitPatchRequest(
    val name: String? = null
)

data class SplitResponse(
    val splitId: Long,
    val name: String,
    val workoutDayCount: Int,
    val exerciseCount: Int
)

data class SplitDetailResponse(
    val splitId: Long,
    val name: String,
    val workoutDays: List<WorkoutDayResponse>
)
