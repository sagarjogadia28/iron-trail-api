package com.irontrail.api.split.repository

import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface TemplateSetRepository : JpaRepository<TemplateSet, Long> {
    fun findByTemplateExerciseIn(templateExercises: List<TemplateExercise>): List<TemplateSet>

    @Query(
        """
         SELECT ts FROM TemplateSet ts, WorkoutDay wd, Split s 
         WHERE ts.templateSetId = :templateSetId AND ts.templateExercise.workoutDayId = wd.workoutDayId 
         AND wd.splitId = s.splitId AND s.ownerId = :ownerId
     """,
    )
    fun findOwnedByTemplateSetId(
        @Param("templateSetId") templateSetId: Long,
        @Param("ownerId") ownerId: Long,
    ): TemplateSet?
}
