package com.irontrail.api.user.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import java.time.OffsetDateTime

@Entity
@Table(name = "users")
class User(
    @Column(unique = true)
    var email: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    var userId: Long = 0

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    var createdAt: OffsetDateTime = OffsetDateTime.now()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is User) return false
        return userId != 0L && userId == other.userId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "User(userId=$userId, email=$email)"
}
