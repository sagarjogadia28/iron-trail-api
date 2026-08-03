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
data class SessionSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_set_id")
    val sessionSetId: Long = 0,

    @Column(name = "sort_order")
    val sortOrder: Int,

    @Enumerated(EnumType.STRING)
    @Column(name = "set_type")
    val setType: SetType,

    @Column(name = "target_reps")
    val targetReps: Int? = null,

    @Column(name = "target_reps_max")
    val targetRepsMax: Int? = null,

    @Column(name = "target_duration_seconds")
    val targetDurationSeconds: Int? = null,

    val reps: Int? = null,

    @Column(name = "weight_kg")
    val weightKg: Double? = null,

    @Column(name = "duration_seconds")
    val durationSeconds: Int? = null,

    @Column(name = "is_completed")
    val isCompleted: Boolean
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_exercise_id")
    lateinit var sessionExercise: SessionExercise
}
