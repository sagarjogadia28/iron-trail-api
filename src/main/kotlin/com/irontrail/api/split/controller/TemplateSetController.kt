package com.irontrail.api.split.controller

import com.irontrail.api.split.dto.TemplateSetRequest
import com.irontrail.api.split.dto.TemplateSetResponse
import com.irontrail.api.split.service.SplitService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/template-sets")
class TemplateSetController(
    private val splitService: SplitService
) {
    @PutMapping("/{templateSetId}")
    fun update(
        @PathVariable templateSetId: Long,
        @Valid @RequestBody request: TemplateSetRequest,
        @AuthenticationPrincipal userId: Long
    ): TemplateSetResponse = splitService.updateTemplateSet(templateSetId, request, userId)

    @DeleteMapping("/{templateSetId}")
    fun delete(@PathVariable templateSetId: Long, @AuthenticationPrincipal userId: Long): ResponseEntity<Void> {
        splitService.deleteTemplateSet(templateSetId, userId)
        return ResponseEntity.noContent().build()
    }
}