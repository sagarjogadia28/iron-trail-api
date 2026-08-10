package com.irontrail.api.split.dto

data class WorkoutDayResponse(
    val workoutDayId: Long,
    val name: String,
    val sortOrder: Int,
    val templateExercises: List<TemplateExerciseResponse>
)