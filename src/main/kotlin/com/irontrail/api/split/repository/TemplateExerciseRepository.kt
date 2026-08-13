package com.irontrail.api.split.repository

import com.irontrail.api.split.model.TemplateExercise
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TemplateExerciseRepository : JpaRepository<TemplateExercise, Long> {
    fun findByWorkoutDayIdIn(workoutDayIds: List<Long>): List<TemplateExercise>

    @Query(
        """
            SELECT te FROM TemplateExercise te, WorkoutDay wd, Split s 
            WHERE te.templateExerciseId = :templateExerciseId AND te.workoutDayId = wd.workoutDayId AND 
            wd.splitId = s.splitId AND s.ownerId = :ownerId
        """
    )
    fun findOwnedByTemplateExerciseId(
        @Param("templateExerciseId") templateExerciseId: Long,
        @Param("ownerId") ownerId: Long
    ): TemplateExercise?
}