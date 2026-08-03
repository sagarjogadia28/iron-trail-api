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
data class SessionExercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_exercise_id")
    val sessionExerciseId: Long = 0,

    @Column(name = "exercise_id")
    val exerciseId: Long? = null,

    @Column(name = "exercise_name_snapshot")
    val exerciseNameSnapshot: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type_snapshot")
    val inputTypeSnapshot: ExerciseInputType,

    @Column(name = "is_rep_range")
    val isRepRange: Boolean,

    @Column(name = "rest_duration_seconds")
    val restDurationSeconds: Int,

    @Column(name = "sort_order")
    val sortOrder: Int,

    val notes: String? = null
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    lateinit var workoutSession: WorkoutSession

    @OneToMany(mappedBy = "sessionExercise", fetch = FetchType.LAZY)
    val sets: MutableList<SessionSet> = mutableListOf()
}