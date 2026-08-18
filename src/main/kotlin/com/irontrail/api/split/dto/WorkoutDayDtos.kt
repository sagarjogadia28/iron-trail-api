package com.irontrail.api.split.dto

import jakarta.validation.constraints.NotBlank

data class WorkoutDayRequest(
    @field:NotBlank
    val name: String,
    val sortOrder: Int
)

data class WorkoutDayPatchRequest(
    val name: String? = null,
    val sortOrder: Int? = null
)

data class WorkoutDayResponse(
    val workoutDayId: Long,
    val name: String,
    val sortOrder: Int,
    val templateExercises: List<TemplateExerciseResponse>
)