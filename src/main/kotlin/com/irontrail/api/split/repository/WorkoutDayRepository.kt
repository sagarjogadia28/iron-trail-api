package com.irontrail.api.split.repository

import com.irontrail.api.split.model.WorkoutDay
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface WorkoutDayRepository : JpaRepository<WorkoutDay, Long> {
    fun findBySplitIdIn(splitIds: List<Long>): List<WorkoutDay>

    @Query("SELECT wd FROM WorkoutDay wd, Split s WHERE wd.workoutDayId = :workoutDayId AND wd.splitId = s.splitId AND s.ownerId = :ownerId")
    fun findOwnedByWorkoutDayId(
        @Param("workoutDayId") workoutDayId: Long,
        @Param("ownerId") ownerId: Long
    ): WorkoutDay?
}