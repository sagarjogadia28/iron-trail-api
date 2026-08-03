package com.irontrail.api.exercise.dto

import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup

data class ExerciseResponse(
    val exerciseId: Long,
    val wgerId: Int?,
    val name: String,
    val muscleGroups: List<MuscleGroup>,
    val equipment: Equipment,
    val inputType: ExerciseInputType,
    val description: String?,
    val imageUrl: String?,
    val ownerId: Long?
)