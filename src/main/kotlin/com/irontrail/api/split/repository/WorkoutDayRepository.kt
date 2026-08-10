package com.irontrail.api.split.repository

import com.irontrail.api.split.model.WorkoutDay
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutDayRepository : JpaRepository<WorkoutDay, Long> {
    fun findBySplitIdIn(splitIds: List<Long>) : List<WorkoutDay>
}