package com.irontrail.api.session.controller

import com.irontrail.api.session.dto.SessionSetPatchRequest
import com.irontrail.api.session.dto.SessionSetResponse
import com.irontrail.api.session.service.WorkoutSessionService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/session-sets")
class SessionSetController(
    private val workoutSessionService: WorkoutSessionService
) {
    @PatchMapping("/{sessionSetId}")
    fun update(
        @PathVariable sessionSetId: Long,
        @Valid @RequestBody request: SessionSetPatchRequest,
        @AuthenticationPrincipal userId: Long
    ): SessionSetResponse = workoutSessionService.updateSessionSet(sessionSetId, request, userId)

    @DeleteMapping("/{sessionSetId}")
    fun delete(@PathVariable sessionSetId: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        workoutSessionService.deleteSessionSet(sessionSetId, userId)
        return ResponseEntity.noContent().build()
    }
}
