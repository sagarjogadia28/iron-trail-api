package com.irontrail.api.split.repository

import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import org.springframework.data.jpa.repository.JpaRepository

interface TemplateSetRepository : JpaRepository<TemplateSet, Long> {
    fun findByTemplateExerciseIn(templateExercises: List<TemplateExercise>): List<TemplateSet>
}
