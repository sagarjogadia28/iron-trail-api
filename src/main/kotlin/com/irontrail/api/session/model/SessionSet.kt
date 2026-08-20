package com.irontrail.api.session.model

import com.irontrail.api.split.model.SetType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "session_sets")
class SessionSet(
    @Column(name = "sort_order")
    var sortOrder: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "set_type")
    var setType: SetType,
    @Column(name = "target_reps")
    var targetReps: Int? = null,
    @Column(name = "target_reps_max")
    var targetRepsMax: Int? = null,
    @Column(name = "target_duration_seconds")
    var targetDurationSeconds: Int? = null,
    var reps: Int? = null,
    @Column(name = "weight_kg")
    var weightKg: Double? = null,
    @Column(name = "duration_seconds")
    var durationSeconds: Int? = null,
    @Column(name = "is_completed")
    var isCompleted: Boolean,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_set_id")
    var sessionSetId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_exercise_id")
    lateinit var sessionExercise: SessionExercise

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionSet) return false
        return sessionSetId != 0L && sessionSetId == other.sessionSetId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "SessionSet(sessionSetId=$sessionSetId, sortOrder=$sortOrder, setType=$setType)"
}
