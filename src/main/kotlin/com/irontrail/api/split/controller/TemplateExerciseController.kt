package com.irontrail.api.split.controller

import com.irontrail.api.split.dto.TemplateExercisePatchRequest
import com.irontrail.api.split.dto.TemplateExerciseRequest
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.TemplateSetRequest
import com.irontrail.api.split.dto.TemplateSetResponse
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
@RequestMapping("/v1/template-exercises")
class TemplateExerciseController(
    private val splitService: SplitService
) {
    @PatchMapping("/{templateExerciseId}")
    fun update(
        @PathVariable templateExerciseId: Long,
        @Valid @RequestBody request: TemplateExercisePatchRequest,
        @AuthenticationPrincipal userId: Long
    ): TemplateExerciseResponse = splitService.updateTemplateExercise(templateExerciseId, request, userId)

    @DeleteMapping("/{templateExerciseId}")
    fun delete(@PathVariable templateExerciseId: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        splitService.deleteTemplateExercise(templateExerciseId, userId)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/{templateExerciseId}/template-sets")
    fun createTemplateSet(
        @PathVariable templateExerciseId: Long,
        @Valid @RequestBody request: TemplateSetRequest,
        @AuthenticationPrincipal userId: Long
    ): ResponseEntity<TemplateSetResponse> {
        val created = splitService.createTemplateSet(templateExerciseId, request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }
}