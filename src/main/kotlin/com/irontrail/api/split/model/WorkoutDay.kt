package com.irontrail.api.split.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "workout_days")
class WorkoutDay(
    @Column(name = "split_id")
    var splitId: Long,
    var name: String,
    @Column(name = "sort_order")
    var sortOrder: Int,
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workout_day_id")
    var workoutDayId: Long = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is WorkoutDay) return false
        return workoutDayId != 0L && workoutDayId == other.workoutDayId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "WorkoutDay(workoutDayId=$workoutDayId, splitId=$splitId, name=$name, sortOrder=$sortOrder)"
}
