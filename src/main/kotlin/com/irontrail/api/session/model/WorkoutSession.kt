package com.irontrail.api.session.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.time.OffsetDateTime

@Entity
@Table(name = "workout_sessions")
data class WorkoutSession(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    val sessionId: Long = 0,

    @Column(name = "owner_id")
    val ownerId: Long,

    @Column(name = "workout_day_id")
    val workoutDayId: Long? = null,

    @Column(name = "split_name_snapshot")
    val splitNameSnapshot: String? = null,

    @Column(name = "workout_day_name_snapshot")
    val workoutDayNameSnapshot: String? = null,

    @Column(name = "started_at")
    val startedAt: OffsetDateTime,

    @Column(name = "ended_at")
    val endedAt: OffsetDateTime? = null,

    @Column(name = "duration_seconds")
    val durationSeconds: Long,

    @Column(name = "total_volume_kg")
    val totalVolumeKg: Double? = null,

    @Column(name = "completed_sets")
    val completedSets: Int? = null,

    @Column(name = "total_sets")
    val totalSets: Int? = null,

    val notes: String? = null,

    @Enumerated(EnumType.STRING)
    val status: SessionStatus
) {
    @OneToMany(mappedBy = "workoutSession", fetch = FetchType.LAZY)
    val sessionExercises: MutableList<SessionExercise> = mutableListOf()
}