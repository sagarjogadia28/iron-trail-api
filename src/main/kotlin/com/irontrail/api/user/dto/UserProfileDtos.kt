package com.irontrail.api.user.dto

import com.irontrail.api.user.model.Gender
import com.irontrail.api.user.model.MeasurementUnit
import com.irontrail.api.user.model.WeightUnit
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.OffsetDateTime

data class UserProfileRequest(
    @field:NotBlank
    @field:Size(min = 2, max = 30)
    val name: String,
    @field:NotNull
    val gender: Gender,
    @field:NotNull
    val weightUnit: WeightUnit,
    @field:NotNull
    val measurementUnit: MeasurementUnit,
)

data class UserProfilePatchRequest(
    @field:Pattern(regexp = ".*\\S.*", message = "must not be blank")
    @field:Size(max = 30)
    val name: String? = null,
    val gender: Gender? = null,
    val weightUnit: WeightUnit? = null,
    val measurementUnit: MeasurementUnit? = null,
    val restTimerNotificationsEnabled: Boolean? = null,
    val activeSplitId: Long? = null,
)

data class UserProfileResponse(
    val userId: Long,
    val name: String,
    val gender: Gender,
    val weightUnit: WeightUnit,
    val measurementUnit: MeasurementUnit,
    val restTimerNotificationsEnabled: Boolean,
    val activeSplitId: Long?,
    val createdAt: OffsetDateTime,
)
