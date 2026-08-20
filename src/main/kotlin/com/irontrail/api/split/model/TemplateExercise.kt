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
class TemplateExercise(
    @Column(name = "workout_day_id")
    var workoutDayId: Long,
    @Column(name = "exercise_id")
    var exerciseId: Long,
    @Column(name = "sort_order")
    var sortOrder: Int,
    @Column(name = "rest_duration_seconds")
    var restDurationSeconds: Int = 90,
    @Column(name = "is_rep_range")
    var isRepRange: Boolean = true,
    var notes: String? = null,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_exercise_id")
    var templateExerciseId: Long = 0

    @OneToMany(mappedBy = "templateExercise", fetch = FetchType.LAZY)
    val sets: MutableList<TemplateSet> = mutableListOf()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemplateExercise) return false
        return templateExerciseId != 0L && templateExerciseId == other.templateExerciseId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String =
        "TemplateExercise(templateExerciseId=$templateExerciseId, workoutDayId=$workoutDayId, exerciseId=$exerciseId)"
}
