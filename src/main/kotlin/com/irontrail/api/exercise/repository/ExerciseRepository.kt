package com.irontrail.api.exercise.repository

import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.MuscleGroup
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ExerciseRepository : JpaRepository<Exercise, Long> {
    companion object {
        private const val VISIBLE = "(e.ownerId IS NULL OR e.ownerId = :ownerId)"
    }

    fun findByExerciseIdAndOwnerId(
        exerciseId: Long,
        ownerId: Long,
    ): Exercise?

    @Query(
        "SELECT e FROM Exercise e WHERE $VISIBLE " +
            "AND LOWER(e.name) LIKE LOWER(CONCAT('%', :search, '%')) ESCAPE '\\' " +
            "AND (:hasMuscleFilter = false OR e.primaryMuscleGroup IN :muscleGroups " +
            "OR EXISTS (SELECT sg FROM e.secondaryMuscleGroups sg WHERE sg IN :muscleGroups))",
    )
    fun findVisibleBySearchAndMuscleGroups(
        @Param("search") search: String,
        @Param("hasMuscleFilter") hasMuscleFilter: Boolean,
        @Param("muscleGroups") muscleGroups: List<MuscleGroup>,
        @Param("ownerId") ownerId: Long,
    ): List<Exercise>

    @Query("SELECT e FROM Exercise e WHERE e.exerciseId = :id AND $VISIBLE")
    fun findVisibleById(
        @Param("id") id: Long,
        @Param("ownerId") ownerId: Long,
    ): Exercise?

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END FROM Exercise e WHERE e.exerciseId = :id AND $VISIBLE")
    fun existsVisibleById(
        @Param("id") id: Long,
        @Param("ownerId") ownerId: Long,
    ): Boolean
}
