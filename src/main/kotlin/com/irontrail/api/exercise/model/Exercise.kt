package com.irontrail.api.exercise.model

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table

@Entity
@Table(name = "exercises")
class Exercise(
    @Column(name = "wger_id")
    var wgerId: Int?,

    var name: String,

    @ElementCollection
    @CollectionTable(
        name = "exercise_muscle_groups",
        joinColumns = [JoinColumn(name = "exercise_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group")
    var muscleGroups: List<MuscleGroup>,

    @Enumerated(EnumType.STRING)
    var equipment: Equipment,

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    var inputType: ExerciseInputType,

    var description: String?,

    @Column(name = "image_url")
    var imageUrl: String?,

    @Column(name = "owner_id")
    var ownerId: Long?
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    var exerciseId: Long = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Exercise) return false
        return exerciseId != 0L && exerciseId == other.exerciseId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Exercise(exerciseId=$exerciseId, name=$name)"
}
