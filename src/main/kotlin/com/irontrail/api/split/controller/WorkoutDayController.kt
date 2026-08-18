package com.irontrail.api.split.controller

import com.irontrail.api.split.dto.TemplateExerciseRequest
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.WorkoutDayDuplicateRequest
import com.irontrail.api.split.dto.WorkoutDayPatchRequest
import com.irontrail.api.split.dto.WorkoutDayRequest
import com.irontrail.api.split.dto.WorkoutDayResponse
import com.irontrail.api.split.service.SplitService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/workout-days")
class WorkoutDayController(
    private val splitService: SplitService
) {
    @PatchMapping("/{workoutDayId}")
    fun update(
        @PathVariable workoutDayId: Long,
        @Valid @RequestBody request: WorkoutDayPatchRequest,
        @AuthenticationPrincipal userId: Long
    ) : WorkoutDayResponse = splitService.updateWorkoutDay(workoutDayId, request, userId)

    @DeleteMapping("/{workoutDayId}")
    fun delete(@PathVariable workoutDayId: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        splitService.deleteWorkoutDay(workoutDayId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{workoutDayId}/duplicate")
    fun duplicate(
        @PathVariable workoutDayId: Long,
        @Valid @RequestBody request: WorkoutDayDuplicateRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<WorkoutDayResponse> {
        val duplicated = splitService.duplicateWorkoutDay(workoutDayId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated)
    }

    @PostMapping("/{workoutDayId}/template-exercises")
    fun createTemplateExercise(
        @PathVariable workoutDayId: Long,
        @Valid @RequestBody request: TemplateExerciseRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TemplateExerciseResponse> {
        val created = splitService.createTemplateExercise(workoutDayId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}