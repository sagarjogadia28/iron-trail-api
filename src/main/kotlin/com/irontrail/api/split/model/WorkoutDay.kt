package com.irontrail.api.split.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "workout_days")
data class WorkoutDay(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workout_day_id")
    val workoutDayId: Long = 0,

    @Column(name = "split_id")
    val splitId: Long,

    val name: String,

    @Column(name = "sort_order")
    val sortOrder: Int
)