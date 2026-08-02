package com.irontrail.api.exercise

import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExerciseService(
    private val exerciseRepository: ExerciseRepository
) {

    fun findAll(muscleGroup: MuscleGroup?): List<ExerciseResponse> =
        (if (muscleGroup != null) exerciseRepository.findByMuscleGroupsContaining(muscleGroup)
        else exerciseRepository.findAll())
            .map { it.toResponse() }

    fun findById(id: Long): ExerciseResponse =
        exerciseRepository.findById(id).orElseThrow { ExerciseNotFoundException(id) }.toResponse()

    fun create(request: ExerciseRequest): ExerciseResponse {
        val exercise = Exercise(
            wgerId = null,
            name = request.name,
            muscleGroups = request.muscleGroups,
            equipment = request.equipment,
            inputType = request.inputType,
            description = request.description,
            imageUrl = null,
            isCustom = true
        )
        return exerciseRepository.save(exercise).toResponse()
    }

    fun update(id: Long, request: ExerciseRequest): ExerciseResponse {
        val existing = exerciseRepository.findById(id).orElseThrow { ExerciseNotFoundException(id) }
        val updated = existing.copy(
            name = request.name,
            muscleGroups = request.muscleGroups,
            equipment = request.equipment,
            inputType = request.inputType,
            description = request.description
        )
        return exerciseRepository.save(updated).toResponse()
    }

    fun delete(id: Long) {
        if (exerciseRepository.existsById(id)) exerciseRepository.deleteById(id)
        else throw ExerciseNotFoundException(id)
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
        isCustom = isCustom
    )
}
