package com.irontrail.api.exercise.repository

import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.MuscleGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    fun findByOwnerIdIsNullOrOwnerId(ownerId: Long): List<Exercise>

    fun findByExerciseIdAndOwnerId(exerciseId: Long, ownerId: Long): Exercise?

    @Query(
        "SELECT e FROM Exercise e WHERE :muscleGroup MEMBER OF e.muscleGroups " + "AND (e.ownerId IS NULL OR e.ownerId = :ownerId)"
    )
    fun findVisibleByMuscleGroup(
        @Param("muscleGroup") muscleGroup: MuscleGroup, @Param("ownerId") ownerId: Long
    ): List<Exercise>

    @Query("SELECT e FROM Exercise e WHERE e.exerciseId = :id AND (e.ownerId IS NULL OR e.ownerId = :ownerId)")
    fun findVisibleById(
        @Param("id") id: Long, @Param("ownerId") ownerId: Long
    ): Exercise?
}