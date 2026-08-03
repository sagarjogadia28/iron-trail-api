package com.irontrail.api.split.model

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
@Table(name = "template_sets")
data class TemplateSet(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_set_id")
    val templateSetId: Long = 0,

    @Column(name = "sort_order")
    val sortOrder: Int,

    @Column(name = "target_reps")
    val targetReps: Int? = null,

    @Column(name = "target_reps_max")
    val targetRepsMax: Int? = null,

    @Column(name = "target_duration_seconds")
    val targetDurationSeconds: Int? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "set_type")
    val setType: SetType
) {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_exercise_id")
    lateinit var templateExercise: TemplateExercise
}