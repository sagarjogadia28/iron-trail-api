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
class TemplateSet(
    @Column(name = "sort_order")
    var sortOrder: Int,
    @Column(name = "target_reps")
    var targetReps: Int? = null,
    @Column(name = "target_reps_max")
    var targetRepsMax: Int? = null,
    @Column(name = "target_duration_seconds")
    var targetDurationSeconds: Int? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "set_type")
    var setType: SetType,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "template_set_id")
    var templateSetId: Long = 0

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_exercise_id")
    lateinit var templateExercise: TemplateExercise

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TemplateSet) return false
        return templateSetId != 0L && templateSetId == other.templateSetId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "TemplateSet(templateSetId=$templateSetId, sortOrder=$sortOrder, setType=$setType)"
}
