package com.irontrail.api.exercise.controller

import com.irontrail.api.exercise.dto.ExercisePatchRequest
import com.irontrail.api.exercise.dto.ExerciseRequest
import com.irontrail.api.exercise.dto.ExerciseResponse
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.exercise.service.ExerciseService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/exercises")
class ExerciseController(
    private val exerciseService: ExerciseService
) {

    @GetMapping
    fun findAll(
        @RequestParam(required = false) muscleGroup: MuscleGroup?,
        @AuthenticationPrincipal userId: Long
    ): List<ExerciseResponse> = exerciseService.findAll(muscleGroup, userId)

    @GetMapping("/{id}")
    fun findById(@PathVariable id: Long, @AuthenticationPrincipal userId: Long): ExerciseResponse =
        exerciseService.findById(id, userId)

    @PostMapping
    fun create(
        @Valid @RequestBody request: ExerciseRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<ExerciseResponse> {
        val created = exerciseService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: ExercisePatchRequest,
        @AuthenticationPrincipal userId: Long
    ): ExerciseResponse = exerciseService.update(id, request, userId)

    @DeleteMapping("/{id}")
    fun delete(@PathVariable id: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        exerciseService.delete(id, userId)
        return ResponseEntity.noContent().build()
    }
}