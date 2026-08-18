package com.irontrail.api.session.controller

import com.irontrail.api.session.dto.SessionExercisePatchRequest
import com.irontrail.api.session.dto.SessionExerciseResponse
import com.irontrail.api.session.dto.SessionSetRequest
import com.irontrail.api.session.dto.SessionSetResponse
import com.irontrail.api.session.service.WorkoutSessionService
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
@RequestMapping("/v1/session-exercises")
class SessionExerciseController(
    private val workoutSessionService: WorkoutSessionService
) {
    @PatchMapping("/{sessionExerciseId}")
    fun update(
        @PathVariable sessionExerciseId: Long,
        @Valid @RequestBody request: SessionExercisePatchRequest,
        @AuthenticationPrincipal userId: Long
    ): SessionExerciseResponse = workoutSessionService.updateSessionExercise(sessionExerciseId, request, userId)

    @DeleteMapping("/{sessionExerciseId}")
    fun delete(@PathVariable sessionExerciseId: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        workoutSessionService.deleteSessionExercise(sessionExerciseId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{sessionExerciseId}/session-sets")
    fun createSessionSet(
        @PathVariable sessionExerciseId: Long,
        @Valid @RequestBody request: SessionSetRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<SessionSetResponse> {
        val created = workoutSessionService.createSessionSet(sessionExerciseId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
