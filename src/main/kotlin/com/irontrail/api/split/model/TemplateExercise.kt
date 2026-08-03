package com.irontrail.api.split.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "template_exercises")
data class TemplateExercise(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_exercise_id")
    val templateExerciseId: Long = 0,

    @Column(name = "workout_day_id")
    val workoutDayId: Long,

    @Column(name = "exercise_id")
    val exerciseId: Long,

    @Column(name = "sort_order")
    val sortOrder: Int,

    @Column(name = "rest_duration_seconds")
    val restDurationSeconds: Int = 90,

    @Column(name = "is_rep_range")
    val isRepRange: Boolean = true,

    val notes: String? = null
) {
    @OneToMany(mappedBy = "templateExercise", fetch = FetchType.LAZY)
    val sets: MutableList<TemplateSet> = mutableListOf()
}