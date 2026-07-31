package com.irontrail.api.exercise

data class Exercise(
    val exerciseId: Long,
    val wgerId: Int?,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: Equipment,
    val inputType: ExerciseInputType,
    val description: String?,
    val imageUrl: String?,
    val isCustom: Boolean
)