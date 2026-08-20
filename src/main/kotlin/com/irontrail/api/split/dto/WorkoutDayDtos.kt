package com.irontrail.api.split.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class WorkoutDayRequest(
    @field:NotBlank
    val name: String,
    val sortOrder: Int,
)

data class WorkoutDayPatchRequest(
    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    val name: String? = null,
    val sortOrder: Int? = null,
)

data class WorkoutDayResponse(
    val workoutDayId: Long,
    val name: String,
    val sortOrder: Int,
    val templateExercises: List<TemplateExerciseResponse>,
)
