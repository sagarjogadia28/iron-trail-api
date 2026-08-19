package com.irontrail.api.home.dto

import com.irontrail.api.session.dto.WorkoutSessionResponse
import java.time.LocalDate

data class NextWorkoutResponse(
    val workoutDayId: Long,
    val workoutDayName: String,
    val splitName: String,
    val exerciseCount: Int
)

data class HomeResponse(
    val nextWorkout: NextWorkoutResponse?,
    val trainedDatesThisMonth: List<LocalDate>,
    val workoutsThisMonth: Int,
    val weekStreak: Int,
    val recentWorkouts: List<WorkoutSessionResponse>
)
