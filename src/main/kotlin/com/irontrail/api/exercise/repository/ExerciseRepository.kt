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
        "SELECT e FROM Exercise e WHERE (e.primaryMuscleGroup = :muscleGroup OR :muscleGroup MEMBER OF e.secondaryMuscleGroups) " +
            "AND (e.ownerId IS NULL OR e.ownerId = :ownerId)"
    )
    fun findVisibleByMuscleGroup(
        @Param("muscleGroup") muscleGroup: MuscleGroup, @Param("ownerId") ownerId: Long
    ): List<Exercise>

    @Query("SELECT e FROM Exercise e WHERE e.exerciseId = :id AND (e.ownerId IS NULL OR e.ownerId = :ownerId)")
    fun findVisibleById(
        @Param("id") id: Long, @Param("ownerId") ownerId: Long
    ): Exercise?

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Exercise e WHERE e.exerciseId = :id AND (e.ownerId IS NULL OR e.ownerId = :ownerId)")
    fun existsVisibleById(
        @Param("id") id: Long, @Param("ownerId") ownerId: Long
    ): Boolean
}