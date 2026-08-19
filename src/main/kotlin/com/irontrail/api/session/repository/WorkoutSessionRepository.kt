package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    fun findByOwnerId(ownerId: Long): List<WorkoutSession>
    fun findBySessionIdAndOwnerId(sessionId: Long, ownerId: Long) : WorkoutSession?
    fun findByOwnerIdAndStatusIn(ownerId: Long, status: List<SessionStatus>) : WorkoutSession?
    fun findByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(
        ownerId: Long, workoutDayId: Long, status: SessionStatus
    ): List<WorkoutSession>
}