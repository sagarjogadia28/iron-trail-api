package com.irontrail.api.exercise.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.dto.ExerciseRequest
import com.irontrail.api.exercise.dto.ExerciseResponse
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.exercise.repository.ExerciseRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExerciseService(
    private val exerciseRepository: ExerciseRepository
) {

    fun findAll(muscleGroup: MuscleGroup?, userId: Long): List<ExerciseResponse> =
        (if (muscleGroup != null) exerciseRepository.findVisibleByMuscleGroup(muscleGroup, userId)
        else exerciseRepository.findByOwnerIdIsNullOrOwnerId(userId))
            .map { it.toResponse() }

    fun findById(id: Long, userId: Long): ExerciseResponse =
        exerciseRepository.findVisibleById(id, userId)?.toResponse()
            ?: throw NotFoundException("Exercise", id)

    fun create(request: ExerciseRequest, userId: Long): ExerciseResponse {
        val exercise = Exercise(
            wgerId = null,
            name = request.name,
            muscleGroups = request.muscleGroups,
            equipment = request.equipment,
            inputType = request.inputType,
            description = request.description,
            imageUrl = null,
            ownerId = userId
        )
        return exerciseRepository.save(exercise).toResponse()
    }

    fun update(id: Long, request: ExerciseRequest, userId: Long): ExerciseResponse {
        val existing = exerciseRepository.findByExerciseIdAndOwnerId(id, userId) ?: throw NotFoundException("Exercise", id)
        existing.name = request.name
        existing.muscleGroups = request.muscleGroups
        existing.equipment = request.equipment
        existing.inputType = request.inputType
        existing.description = request.description
        return existing.toResponse()
    }

    fun delete(id: Long, userId: Long) {
        val existing = exerciseRepository.findByExerciseIdAndOwnerId(id, userId) ?: throw NotFoundException("Exercise", id)
        exerciseRepository.delete(existing)
    }

    private fun Exercise.toResponse() = ExerciseResponse(
        exerciseId = exerciseId,
        wgerId = wgerId,
        name = name,
        muscleGroups = muscleGroups,
        equipment = equipment,
        inputType = inputType,
        description = description,
        imageUrl = imageUrl,
        ownerId = ownerId
    )
}
