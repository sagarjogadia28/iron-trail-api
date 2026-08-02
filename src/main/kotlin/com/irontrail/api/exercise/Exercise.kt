package com.irontrail.api.exercise

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
data class Exercise(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "exercise_id")
    val exerciseId: Long = 0,

    @Column(name = "wger_id")
    val wgerId: Int?,

    val name: String,

    @ElementCollection
    @CollectionTable(
        name = "exercise_muscle_groups",
        joinColumns = [JoinColumn(name = "exercise_id")]
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "muscle_group")
    val muscleGroups: List<MuscleGroup>,

    @Enumerated(EnumType.STRING)
    val equipment: Equipment,

    @Enumerated(EnumType.STRING)
    @Column(name = "input_type")
    val inputType: ExerciseInputType,

    val description: String?,

    @Column(name = "image_url")
    val imageUrl: String?,

    @Column(name = "is_custom")
    val isCustom: Boolean
)