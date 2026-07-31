package com.irontrail.api.exercise

import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

@Service
class ExerciseService {

    private val exercises = ConcurrentHashMap<Long, Exercise>()
    private val idSequence = AtomicLong(0)

    init {
        seed("Bench Press", listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), Equipment.BARBELL, ExerciseInputType.REPS, "Flat barbell bench press")
        seed("Back Squat", listOf(MuscleGroup.QUADS, MuscleGroup.GLUTES), Equipment.BARBELL, ExerciseInputType.REPS, "Barbell back squat")
        seed("Plank", listOf(MuscleGroup.CORE), Equipment.BODYWEIGHT, ExerciseInputType.TIMED, "Hold a plank position")
        seed("Pull-up", listOf(MuscleGroup.BACK, MuscleGroup.BICEPS), Equipment.BODYWEIGHT, ExerciseInputType.REPS, "Bodyweight pull-up")
    }

    fun findAll(muscleGroup: MuscleGroup?): List<ExerciseResponse> =
        exercises.values
            .filter { muscleGroup == null || muscleGroup in it.muscleGroups }
            .map { it.toResponse() }

    fun findById(id: Long): ExerciseResponse =
        (exercises[id] ?: throw ExerciseNotFoundException(id)).toResponse()

    fun create(request: ExerciseRequest): ExerciseResponse {
        val id = idSequence.incrementAndGet()
        val exercise = Exercise(
            exerciseId = id,
            wgerId = null,
            name = request.name,
            muscleGroups = request.muscleGroups,
            equipment = request.equipment,
            inputType = request.inputType,
            description = request.description,
            imageUrl = null,
            isCustom = true
        )
        exercises[id] = exercise
        return exercise.toResponse()
    }

    fun update(id: Long, request: ExerciseRequest): ExerciseResponse {
        val existing = exercises[id] ?: throw ExerciseNotFoundException(id)
        val updated = existing.copy(
            name = request.name,
            muscleGroups = request.muscleGroups,
            equipment = request.equipment,
            inputType = request.inputType,
            description = request.description
        )
        exercises[id] = updated
        return updated.toResponse()
    }

    fun delete(id: Long) {
        if (exercises.remove(id) == null) {
            throw ExerciseNotFoundException(id)
        }
    }

    private fun seed(
        name: String,
        muscleGroups: List<MuscleGroup>,
        equipment: Equipment,
        inputType: ExerciseInputType,
        description: String
    ) {
        val id = idSequence.incrementAndGet()
        exercises[id] = Exercise(
            exerciseId = id,
            wgerId = null,
            name = name,
            muscleGroups = muscleGroups,
            equipment = equipment,
            inputType = inputType,
            description = description,
            imageUrl = null,
            isCustom = false
        )
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