package com.irontrail.api.user.controller

import com.irontrail.api.user.dto.UserProfilePatchRequest
import com.irontrail.api.user.dto.UserProfileRequest
import com.irontrail.api.user.dto.UserProfileResponse
import com.irontrail.api.user.service.UserProfileService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/profile")
class UserProfileController(
    private val userProfileService: UserProfileService
) {
    @GetMapping
    fun findByUserId(@AuthenticationPrincipal userId: Long): UserProfileResponse =
        userProfileService.findByUserId(userId)

    @PostMapping
    fun create(
        @Valid @RequestBody request: UserProfileRequest, @AuthenticationPrincipal userId: Long
    ): ResponseEntity<UserProfileResponse> {
        val created = userProfileService.create(request, userId)
        return ResponseEntity.status(HttpStatus.CREATED).body(created)
    }

    @PatchMapping
    fun update(
        @Valid @RequestBody request: UserProfilePatchRequest, @AuthenticationPrincipal userId: Long
    ): UserProfileResponse = userProfileService.update(request, userId)
}
