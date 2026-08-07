package com.irontrail.api.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "user_profile")
class UserProfile(
    var name: String,

    @Enumerated(EnumType.STRING)
    var gender: Gender,

    @Enumerated(EnumType.STRING)
    @Column(name = "weight_unit")
    var weightUnit: WeightUnit,

    @Enumerated(EnumType.STRING)
    @Column(name = "measurement_unit")
    var measurementUnit: MeasurementUnit,

    @Column(name = "profile_image_path")
    var profileImagePath: String?,

    @Column(name = "active_split_id")
    var activeSplitId: Long? = null
) {
    @Id
    @Column(name = "user_id")
    var userId: Long = 0

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is UserProfile) return false
        return userId != 0L && userId == other.userId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "UserProfile(userId=$userId, name=$name)"
}
