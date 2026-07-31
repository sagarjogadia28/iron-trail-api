package com.irontrail.api.exercise

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class ExerciseRequest(
    @field:NotBlank
    val name: String,

    @field:NotEmpty
    val muscleGroups: List<MuscleGroup>,

    @field:NotNull
    val equipment: Equipment,

    @field:NotNull
    val inputType: ExerciseInputType,

    val description: String?
)