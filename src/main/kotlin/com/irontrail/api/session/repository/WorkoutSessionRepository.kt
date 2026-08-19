package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import org.springframework.data.jpa.repository.JpaRepository
import java.time.OffsetDateTime

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long> {
    fun findByOwnerId(ownerId: Long): List<WorkoutSession>
    fun findBySessionIdAndOwnerId(sessionId: Long, ownerId: Long) : WorkoutSession?
    fun findByOwnerIdAndStatusIn(ownerId: Long, status: List<SessionStatus>) : WorkoutSession?
    fun findTop2ByOwnerIdAndWorkoutDayIdAndStatusOrderByStartedAtDesc(
        ownerId: Long, workoutDayId: Long, status: SessionStatus
    ): List<WorkoutSession>
    fun findByOwnerIdAndStatusAndStartedAtAfter(
        ownerId: Long, status: SessionStatus, after: OffsetDateTime
    ): List<WorkoutSession>
    fun findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(ownerId: Long, status: SessionStatus): List<WorkoutSession>
    fun findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
        ownerId: Long, workoutDayIds: List<Long>, status: SessionStatus
    ): WorkoutSession?
}