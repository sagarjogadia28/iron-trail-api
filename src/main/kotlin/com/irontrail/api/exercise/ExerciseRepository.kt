package com.irontrail.api.exercise

import org.springframework.data.jpa.repository.JpaRepository

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByMuscleGroupsContaining(muscleGroup: MuscleGroup): List<Exercise>
}