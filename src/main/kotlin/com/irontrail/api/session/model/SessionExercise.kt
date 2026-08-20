package com.irontrail.api.session.model

import com.irontrail.api.exercise.model.ExerciseInputType
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
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "session_exercises")
class SessionExercise(
    @Column(name = "exercise_id")
    var exerciseId: Long? = null,
    @Column(name = "exercise_name_snapshot")
    var exerciseNameSnapshot: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "input_type_snapshot")
    var inputTypeSnapshot: ExerciseInputType,
    @Column(name = "is_rep_range")
    var isRepRange: Boolean,
    @Column(name = "rest_duration_seconds")
    var restDurationSeconds: Int,
    @Column(name = "sort_order")
    var sortOrder: Int,
    var notes: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_exercise_id")
    var sessionExerciseId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    lateinit var workoutSession: WorkoutSession

    @OneToMany(mappedBy = "sessionExercise", fetch = FetchType.LAZY)
    val sets: MutableList<SessionSet> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SessionExercise) return false
        return sessionExerciseId != 0L && sessionExerciseId == other.sessionExerciseId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "SessionExercise(sessionExerciseId=$sessionExerciseId, exerciseNameSnapshot=$exerciseNameSnapshot)"
}
