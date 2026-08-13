package com.irontrail.api.split.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.repository.ExerciseRepository
import com.irontrail.api.split.dto.SplitDetailResponse
import com.irontrail.api.split.dto.SplitRequest
import com.irontrail.api.split.dto.SplitResponse
import com.irontrail.api.split.dto.TemplateExerciseRequest
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.TemplateSetRequest
import com.irontrail.api.split.dto.TemplateSetResponse
import com.irontrail.api.split.dto.WorkoutDayRequest
import com.irontrail.api.split.dto.WorkoutDayResponse
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
    private val templateSetRepository: TemplateSetRepository,
    private val exerciseRepository: ExerciseRepository,
    private val ownershipResolver: SplitOwnershipResolver
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
        val split = ownershipResolver.getOwnedSplit(splitId, userId)
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
        val split = ownershipResolver.getOwnedSplit(splitId, userId)
        split.name = request.name
        val workoutDays = buildWorkoutDayTree(listOf(split)).getValue(split.splitId)
        return SplitDetailResponse(splitId = split.splitId, name = split.name, workoutDays = workoutDays)
    }

    fun delete(splitId: Long, userId: Long) {
        val split = ownershipResolver.getOwnedSplit(splitId, userId)
        splitRepository.delete(split)
    }

    //WorkoutDay
    fun createWorkoutDay(splitId: Long, request: WorkoutDayRequest, userId: Long): WorkoutDayResponse {
        ownershipResolver.getOwnedSplit(splitId, userId)
        val saved = workoutDayRepository.save(
            WorkoutDay(splitId = splitId, name = request.name, sortOrder = request.sortOrder)
        )
        return saved.toResponse(emptyList())
    }

    fun updateWorkoutDay(workoutDayId: Long, request: WorkoutDayRequest, userId: Long): WorkoutDayResponse {
        val workoutDay = ownershipResolver.getOwnedWorkoutDay(workoutDayId, userId)
        workoutDay.name = request.name
        workoutDay.sortOrder = request.sortOrder
        return workoutDay.toResponse(buildTemplateExerciseResponses(workoutDay))
    }

    fun deleteWorkoutDay(workoutDayId: Long, userId: Long) {
        val workoutDay = ownershipResolver.getOwnedWorkoutDay(workoutDayId, userId)
        workoutDayRepository.delete(workoutDay)
    }

    //TemplateExercise
    fun createTemplateExercise(workoutDayId: Long, request: TemplateExerciseRequest, userId: Long): TemplateExerciseResponse {
        ownershipResolver.getOwnedWorkoutDay(workoutDayId, userId)
        if (!exerciseRepository.existsVisibleById(request.exerciseId, userId))
            throw NotFoundException("Exercise", request.exerciseId)

        val saved = templateExerciseRepository.save(
            TemplateExercise(
                workoutDayId = workoutDayId,
                exerciseId = request.exerciseId,
                sortOrder = request.sortOrder,
                restDurationSeconds = request.restDurationSeconds,
                isRepRange = request.isRepRange,
                notes = request.notes
            )
        )
        return saved.toResponse(emptyList())
    }

    fun updateTemplateExercise(templateExerciseId: Long, request: TemplateExerciseRequest, userId: Long): TemplateExerciseResponse {
        val templateExercise = ownershipResolver.getOwnedTemplateExercise(templateExerciseId, userId)
        if (!exerciseRepository.existsVisibleById(request.exerciseId, userId))
            throw NotFoundException("Exercise", request.exerciseId)

        templateExercise.exerciseId = request.exerciseId
        templateExercise.sortOrder = request.sortOrder
        templateExercise.restDurationSeconds = request.restDurationSeconds
        templateExercise.isRepRange = request.isRepRange
        templateExercise.notes = request.notes
        return templateExercise.toResponse(buildTemplateSetResponses(templateExercise))
    }

    fun deleteTemplateExercise(templateExerciseId: Long, userId: Long) {
        val templateExercise = ownershipResolver.getOwnedTemplateExercise(templateExerciseId, userId)
        templateExerciseRepository.delete(templateExercise)
    }

    //TemplateSet
    fun createTemplateSet(templateExerciseId: Long, request: TemplateSetRequest, userId: Long): TemplateSetResponse {
        val parentExercise = ownershipResolver.getOwnedTemplateExercise(templateExerciseId, userId)
        val saved = templateSetRepository.save(
            TemplateSet(
                sortOrder = request.sortOrder,
                targetReps = request.targetReps,
                targetRepsMax = request.targetRepsMax,
                targetDurationSeconds = request.targetDurationSeconds,
                setType = request.setType
            ).apply { templateExercise = parentExercise }
        )
        return saved.toResponse()
    }

    fun updateTemplateSet(templateSetId: Long, request: TemplateSetRequest, userId: Long): TemplateSetResponse {
        val templateSet = ownershipResolver.getOwnedTemplateSet(templateSetId, userId)
        templateSet.sortOrder = request.sortOrder
        templateSet.targetReps = request.targetReps
        templateSet.targetRepsMax = request.targetRepsMax
        templateSet.targetDurationSeconds = request.targetDurationSeconds
        templateSet.setType = request.setType
        return templateSet.toResponse()
    }

    fun deleteTemplateSet(templateSetId: Long, userId: Long) {
        val templateSet = ownershipResolver.getOwnedTemplateSet(templateSetId, userId)
        templateSetRepository.delete(templateSet)
    }

    private fun buildWorkoutDayTree(splits: List<Split>): Map<Long, List<WorkoutDayResponse>> {
        if (splits.isEmpty()) return emptyMap()
        val splitIds = splits.map { it.splitId }

        val workoutDays = workoutDayRepository.findBySplitIdIn(splitIds)
        val exercisesByDayId = buildTemplateExerciseTree(workoutDays.map { it.workoutDayId })
        val daysBySplitId = workoutDays.groupBy { it.splitId }

        return splitIds.associateWith { splitId ->
            daysBySplitId[splitId].orEmpty().sortedBy { it.sortOrder }.map { day ->
                day.toResponse(exercisesByDayId.getValue(day.workoutDayId))
            }
        }
    }

    private fun buildTemplateExerciseTree(workoutDayIds: List<Long>) : Map<Long, List<TemplateExerciseResponse>> {
        if (workoutDayIds.isEmpty()) return emptyMap()
        val templateExercises = templateExerciseRepository.findByWorkoutDayIdIn(workoutDayIds)
        val templateSets = templateSetRepository.findByTemplateExerciseIn(templateExercises)
        val setsByExerciseId = templateSets.groupBy { it.templateExercise.templateExerciseId }
        val exercisesByDayId = templateExercises.groupBy { it.workoutDayId }

        return workoutDayIds.associateWith { dayId ->
            exercisesByDayId[dayId].orEmpty().sortedBy { it.sortOrder }.map { te ->
                val sets = setsByExerciseId[te.templateExerciseId].orEmpty().sortedBy { it.sortOrder }.map { it.toResponse() }
                te.toResponse(sets)
            }
        }
    }

    private fun buildTemplateExerciseResponses(workoutDay: WorkoutDay): List<TemplateExerciseResponse> =
       buildTemplateExerciseTree(listOf(workoutDay.workoutDayId)).getValue(workoutDay.workoutDayId)

    private fun buildTemplateSetResponses(templateExercise: TemplateExercise): List<TemplateSetResponse> =
        templateExercise.sets.sortedBy { it.sortOrder }.map { it.toResponse() }

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
