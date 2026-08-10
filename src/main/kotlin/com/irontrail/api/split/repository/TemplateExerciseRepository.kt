package com.irontrail.api.split.repository

import com.irontrail.api.split.model.TemplateExercise
import org.springframework.data.jpa.repository.JpaRepository

interface TemplateExerciseRepository : JpaRepository<TemplateExercise, Long> {
    fun findByWorkoutDayIdIn(workoutDayIds: List<Long>): List<TemplateExercise>
}