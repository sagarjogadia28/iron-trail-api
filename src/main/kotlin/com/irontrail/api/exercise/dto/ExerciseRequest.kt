package com.irontrail.api.exercise.dto

import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull

data class ExerciseRequest(
    @field:NotBlank
    val name: String,

    @field:NotNull
    val primaryMuscleGroup: MuscleGroup,

    val secondaryMuscleGroups: List<MuscleGroup> = emptyList(),

    @field:NotNull
    val equipment: Equipment,

    @field:NotNull
    val inputType: ExerciseInputType,

    val description: String?
)

// Merge-patch semantics: a null/absent field means "leave unchanged." This means an
// already-set nullable field like `description` cannot be cleared back to null via PATCH
// with this simple approach (would need a JSON Merge Patch-aware wrapper type to do that).
data class ExercisePatchRequest(
    val name: String? = null,
    val primaryMuscleGroup: MuscleGroup? = null,
    val secondaryMuscleGroups: List<MuscleGroup>? = null,
    val equipment: Equipment? = null,
    val inputType: ExerciseInputType? = null,
    val description: String? = null
)
