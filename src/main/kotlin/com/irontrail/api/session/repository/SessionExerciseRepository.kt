package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.WorkoutSession
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SessionExerciseRepository : JpaRepository<SessionExercise, Long> {
    fun findByWorkoutSessionIn(workoutSession: List<WorkoutSession>): List<SessionExercise>

    @Query("SELECT se FROM SessionExercise se WHERE se.sessionExerciseId = :sessionExerciseId AND se.workoutSession.ownerId = :ownerId")
    fun findOwnedBySessionExerciseId(
        @Param("sessionExerciseId") sessionExerciseId: Long,
        @Param("ownerId") ownerId: Long,
    ): SessionExercise?
}