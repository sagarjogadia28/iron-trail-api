package com.irontrail.api.session.controller

import com.irontrail.api.session.dto.SessionExerciseRequest
import com.irontrail.api.session.dto.SessionExerciseResponse
import com.irontrail.api.session.dto.WorkoutSessionDetailResponse
import com.irontrail.api.session.dto.WorkoutSessionPatchRequest
import com.irontrail.api.session.dto.WorkoutSessionRequest
import com.irontrail.api.session.dto.WorkoutSessionResponse
import com.irontrail.api.session.service.WorkoutSessionService
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
@RequestMapping("/v1/workout-sessions")
class WorkoutSessionController(
    private val workoutSessionService: WorkoutSessionService,
) {
    @GetMapping
    fun findAll(
        @RequestParam(required = false) splitName: String?,
        @AuthenticationPrincipal userId: Long,
    ): List<WorkoutSessionResponse> = workoutSessionService.findAll(userId, splitName)

    @GetMapping("/active")
    fun findActive(
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<WorkoutSessionResponse> {
        val active = workoutSessionService.findActive(userId) ?: return ResponseEntity.noContent().build()
        return ResponseEntity.ok(active)
    }

    @GetMapping("/{sessionId}")
    fun findById(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userId: Long,
    ): WorkoutSessionDetailResponse = workoutSessionService.findById(sessionId, userId)

    @PostMapping
    fun create(
        @Valid @RequestBody request: WorkoutSessionRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<WorkoutSessionDetailResponse> {
        val created = workoutSessionService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping("/{sessionId}")
    fun update(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: WorkoutSessionPatchRequest,
        @AuthenticationPrincipal userId: Long,
    ): WorkoutSessionDetailResponse = workoutSessionService.update(sessionId, request, userId)

    @DeleteMapping("/{sessionId}")
    fun delete(
        @PathVariable sessionId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<Void> {
        workoutSessionService.delete(sessionId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{sessionId}/session-exercises")
    fun createSessionExercise(
        @PathVariable sessionId: Long,
        @Valid @RequestBody request: SessionExerciseRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SessionExerciseResponse> {
        val created = workoutSessionService.createSessionExercise(sessionId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
