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
class WorkoutSession(
    @Column(name = "owner_id")
    var ownerId: Long,

    @Column(name = "workout_day_id")
    var workoutDayId: Long? = null,

    @Column(name = "split_name_snapshot")
    var splitNameSnapshot: String? = null,

    @Column(name = "workout_day_name_snapshot")
    var workoutDayNameSnapshot: String? = null,

    @Column(name = "started_at")
    var startedAt: OffsetDateTime,

    @Column(name = "duration_seconds")
    var durationSeconds: Long,

    @Column(name = "total_volume_kg")
    var totalVolumeKg: Double? = null,

    @Column(name = "completed_sets")
    var completedSets: Int? = null,

    @Column(name = "total_sets")
    var totalSets: Int? = null,

    var notes: String? = null,

    @Enumerated(EnumType.STRING)
    var status: SessionStatus
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    var sessionId: Long = 0

    @OneToMany(mappedBy = "workoutSession", fetch = FetchType.LAZY)
    val sessionExercises: MutableList<SessionExercise> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkoutSession) return false
        return sessionId != 0L && sessionId == other.sessionId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "WorkoutSession(sessionId=$sessionId, ownerId=$ownerId, status=$status)"
}
