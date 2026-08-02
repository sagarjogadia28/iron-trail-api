package com.irontrail.api.exercise.repository

import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.MuscleGroup
import org.springframework.data.jpa.repository.JpaRepository

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByMuscleGroupsContaining(muscleGroup: MuscleGroup): List<Exercise>
}