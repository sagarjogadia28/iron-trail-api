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

    fun getOwnedWorkoutDay(workoutDayId: Long, userId: Long): WorkoutDay =
        workoutDayRepository.findOwnedByWorkoutDayId(workoutDayId, userId) ?: throw NotFoundException(
            "WorkoutDay",
            workoutDayId
        )

    fun getOwnedTemplateExercise(templateExerciseId: Long, userId: Long): TemplateExercise =
        templateExerciseRepository.findOwnedByTemplateExerciseId(templateExerciseId, userId) ?: throw NotFoundException(
            "TemplateExercise",
            templateExerciseId
        )

    fun getOwnedTemplateSet(templateSetId: Long, userId: Long): TemplateSet =
        templateSetRepository.findOwnedByTemplateSetId(templateSetId, userId) ?: throw NotFoundException(
            "TemplateSet",
            templateSetId
        )
}
