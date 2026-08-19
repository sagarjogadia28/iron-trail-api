package com.irontrail.api.user.service

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.user.dto.UserProfilePatchRequest
import com.irontrail.api.user.dto.UserProfileRequest
import com.irontrail.api.user.dto.UserProfileResponse
import com.irontrail.api.user.model.UserProfile
import com.irontrail.api.user.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class UserProfileService(
    private val userProfileRepository: UserProfileRepository,
    private val splitRepository: SplitRepository
) {
    fun create(request: UserProfileRequest, userId: Long): UserProfileResponse {
        if (userProfileRepository.existsById(userId)) {
            throw ConflictException("Profile already exists")
        }

        val profile = UserProfile(
            name = request.name,
            gender = request.gender,
            weightUnit = request.weightUnit,
            measurementUnit = request.measurementUnit,
            restTimerNotificationsEnabled = true,
            profileImagePath = null
        ).apply { this.userId = userId }

        return userProfileRepository.save(profile).toResponse()
    }

    fun findByUserId(userId: Long): UserProfileResponse =
        userProfileRepository.findById(userId).orElseThrow { NotFoundException("UserProfile", userId) }.toResponse()

    fun update(request: UserProfilePatchRequest, userId: Long): UserProfileResponse {
        val profile = userProfileRepository.findById(userId).orElseThrow { NotFoundException("UserProfile", userId) }
        request.name?.let { profile.name = it }
        request.gender?.let { profile.gender = it }
        request.weightUnit?.let { profile.weightUnit = it }
        request.measurementUnit?.let { profile.measurementUnit = it }
        request.restTimerNotificationsEnabled?.let { profile.restTimerNotificationsEnabled = it }
        request.activeSplitId?.let { splitId ->
            splitRepository.findBySplitIdAndOwnerId(splitId, userId) ?: throw NotFoundException("Split", splitId)
            profile.activeSplitId = splitId
        }
        return profile.toResponse()
    }

    private fun UserProfile.toResponse() = UserProfileResponse(
        userId = userId,
        name = name,
        gender = gender,
        weightUnit = weightUnit,
        measurementUnit = measurementUnit,
        restTimerNotificationsEnabled = restTimerNotificationsEnabled,
        activeSplitId = activeSplitId,
        createdAt = createdAt
    )
}
