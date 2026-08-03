package com.irontrail.api.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "user_profile")
data class UserProfile(
    @Id
    @Column(name = "user_id")
    val userId: Long,

    val name: String,

    @Enumerated(EnumType.STRING)
    val gender: Gender,

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit")
    val weightUnit: WeightUnit,

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit")
    val measurementUnit: MeasurementUnit,

    @Column(name = "profile_image_path")
    val profileImagePath: String?,

    @Column(name = "active_split_id")
    val activeSplitId: Long? = null,

    @Column(name = "created_at")
    val createdAt: LocalDateTime = LocalDateTime.now()
)