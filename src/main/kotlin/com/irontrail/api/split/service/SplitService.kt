package com.irontrail.api.split.service

import com.irontrail.api.split.dto.SplitDetailResponse
import com.irontrail.api.split.dto.SplitRequest
import com.irontrail.api.split.dto.SplitResponse
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.TemplateSetResponse
import com.irontrail.api.split.dto.WorkoutDayResponse
import com.irontrail.api.split.exception.SplitNotFoundException
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.TemplateExerciseRepository
import com.irontrail.api.split.repository.TemplateSetRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class SplitService(
    private val splitRepository: SplitRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val templateExerciseRepository: TemplateExerciseRepository,
    private val templateSetRepository: TemplateSetRepository
) {
    fun findAll(userId: Long): List<SplitResponse> {
        val splits = splitRepository.findByOwnerId(userId)
        val workoutDaysBySplitId = buildWorkoutDayTree(splits)
        return splits.map { split ->
            val workoutDays = workoutDaysBySplitId.getValue(split.splitId)
            SplitResponse(
                splitId = split.splitId,
                name = split.name,
                workoutDayCount = workoutDays.size,
                exerciseCount = workoutDays.sumOf { it.templateExercises.size }
            )
        }
    }

    fun findById(splitId: Long, userId: Long): SplitDetailResponse {
        val split = splitRepository.findBySplitIdAndOwnerId(splitId, userId) ?: throw SplitNotFoundException(splitId)
        val workoutDays = buildWorkoutDayTree(listOf(split)).getValue(split.splitId)
        return SplitDetailResponse(
            splitId = split.splitId,
            name = split.name,
            workoutDays = workoutDays
        )
    }

    fun create(request: SplitRequest, userId: Long): SplitDetailResponse {
        val saved = splitRepository.save(Split(ownerId = userId, name = request.name))
        return SplitDetailResponse(splitId = saved.splitId, name = saved.name, workoutDays = emptyList())
    }

    fun update(splitId: Long, request: SplitRequest, userId: Long): SplitDetailResponse {
        val split = splitRepository.findBySplitIdAndOwnerId(splitId, userId) ?: throw SplitNotFoundException(splitId)
        split.name = request.name
        val workoutDays = buildWorkoutDayTree(listOf(split)).getValue(split.splitId)
        return SplitDetailResponse(splitId = split.splitId, name = split.name, workoutDays = workoutDays)
    }

    fun delete(splitId: Long, userId: Long) {
        val split = splitRepository.findBySplitIdAndOwnerId(splitId, userId) ?: throw SplitNotFoundException(splitId)
        splitRepository.delete(split)
    }

    private fun buildWorkoutDayTree(splits: List<Split>): Map<Long, List<WorkoutDayResponse>> {
        if (splits.isEmpty()) return emptyMap()
        val splitIds = splits.map { it.splitId }

        val workoutDays = workoutDayRepository.findBySplitIdIn(splitIds)
        val templateExercises = templateExerciseRepository.findByWorkoutDayIdIn(workoutDays.map { it.workoutDayId })
        val templateSets = templateSetRepository.findByTemplateExerciseIn(templateExercises)

        val daysBySplitId = workoutDays.groupBy { it.splitId }
        val exercisesByDayId = templateExercises.groupBy { it.workoutDayId }
        val setsByExerciseId = templateSets.groupBy { it.templateExercise.templateExerciseId }

        return splitIds.associateWith { splitId ->
            daysBySplitId[splitId].orEmpty().sortedBy { it.sortOrder }.map { day ->
                val exercises = exercisesByDayId[day.workoutDayId].orEmpty().sortedBy { it.sortOrder }.map { te ->
                    val sets = setsByExerciseId[te.templateExerciseId].orEmpty().sortedBy { it.sortOrder }
                        .map { it.toResponse() }
                    te.toResponse(sets)
                }
                day.toResponse(exercises)
            }
        }
    }

    private fun WorkoutDay.toResponse(templateExercises: List<TemplateExerciseResponse>) = WorkoutDayResponse(
        workoutDayId = workoutDayId,
        name = name,
        sortOrder = sortOrder,
        templateExercises = templateExercises
    )

    private fun TemplateExercise.toResponse(templateSets: List<TemplateSetResponse>) = TemplateExerciseResponse(
        templateExerciseId = templateExerciseId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        restDurationSeconds = restDurationSeconds,
        isRepRange = isRepRange,
        notes = notes,
        templateSets = templateSets
    )

    private fun TemplateSet.toResponse() = TemplateSetResponse(
        templateSetId = templateSetId,
        sortOrder = sortOrder,
        targetReps = targetReps,
        targetRepsMax = targetRepsMax,
        targetDurationSeconds = targetDurationSeconds,
        setType = setType
    )
}