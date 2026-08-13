package com.irontrail.api.split.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.TemplateExerciseRepository
import com.irontrail.api.split.repository.TemplateSetRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Component

@Component
class SplitOwnershipResolver(
    private val splitRepository: SplitRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val templateExerciseRepository: TemplateExerciseRepository,
    private val templateSetRepository: TemplateSetRepository
) {
    fun getOwnedSplit(splitId: Long, userId: Long): Split =
        splitRepository.findBySplitIdAndOwnerId(splitId, userId) ?: throw NotFoundException("Split", splitId)

    fun getOwnedWorkoutDay(workoutDayId: Long, userId: Long): WorkoutDay {
        val workoutDay = workoutDayRepository.getOrThrow(workoutDayId, "WorkoutDay")
        try {
            getOwnedSplit(workoutDay.splitId, userId)
        } catch (e: NotFoundException) {
            throw NotFoundException("WorkoutDay", workoutDayId)
        }
        return workoutDay
    }

    fun getOwnedTemplateExercise(templateExerciseId: Long, userId: Long): TemplateExercise {
        val templateExercise = templateExerciseRepository.getOrThrow(templateExerciseId, "TemplateExercise")
        try {
            getOwnedWorkoutDay(templateExercise.workoutDayId, userId)
        } catch (e: NotFoundException) {
            throw NotFoundException("TemplateExercise", templateExerciseId)
        }
        return templateExercise
    }

    fun getOwnedTemplateSet(templateSetId: Long, userId: Long): TemplateSet {
        val templateSet = templateSetRepository.getOrThrow(templateSetId, "TemplateSet")
        try {
            getOwnedTemplateExercise(templateSet.templateExercise.templateExerciseId, userId)
        } catch (e: NotFoundException) {
            throw NotFoundException("TemplateSet", templateSetId)
        }
        return templateSet
    }

    private fun <T : Any> JpaRepository<T, Long>.getOrThrow(id: Long, resource: String): T =
        findById(id).orElseThrow { NotFoundException(resource, id) }
}
