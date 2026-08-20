package com.irontrail.api.split.controller

import com.irontrail.api.split.dto.SplitDetailResponse
import com.irontrail.api.split.dto.SplitPatchRequest
import com.irontrail.api.split.dto.SplitRequest
import com.irontrail.api.split.dto.SplitResponse
import com.irontrail.api.split.dto.WorkoutDayRequest
import com.irontrail.api.split.dto.WorkoutDayResponse
import com.irontrail.api.split.service.SplitService
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/splits")
class SplitController(
    private val splitService: SplitService,
) {
    @GetMapping
    fun findAll(
        @AuthenticationPrincipal userId: Long,
    ): List<SplitResponse> = splitService.findAll(userId)

    @GetMapping("/{splitId}")
    fun findById(
        @PathVariable splitId: Long,
        @AuthenticationPrincipal userId: Long,
    ): SplitDetailResponse = splitService.findById(splitId, userId)

    @PostMapping
    fun create(
        @Valid @RequestBody request: SplitRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SplitDetailResponse> {
        val created = splitService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping("/{splitId}")
    fun update(
        @PathVariable splitId: Long,
        @Valid @RequestBody request: SplitPatchRequest,
        @AuthenticationPrincipal userId: Long,
    ): SplitDetailResponse = splitService.update(splitId, request, userId)

    @DeleteMapping("/{splitId}")
    fun delete(
        @PathVariable splitId: Long,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<Void> {
        splitService.delete(splitId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{splitId}/duplicate")
    fun duplicate(
        @PathVariable splitId: Long,
        @Valid @RequestBody request: SplitRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<SplitDetailResponse> {
        val duplicated = splitService.duplicateSplit(splitId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(duplicated)
    }

    @PostMapping("/{splitId}/workout-days")
    fun createWorkoutDay(
        @PathVariable splitId: Long,
        @Valid @RequestBody request: WorkoutDayRequest,
        @AuthenticationPrincipal userId: Long,
    ): ResponseEntity<WorkoutDayResponse> {
        val created = splitService.createWorkoutDay(splitId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}
