package com.irontrail.api.split.model

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "splits")
class Split(
    @Column(name = "owner_id")
    var ownerId: Long,

    var name: String
) {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "split_id")
    var splitId: Long = 0

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Split) return false
        return splitId != 0L && splitId == other.splitId
    }

    override fun hashCode(): Int = javaClass.hashCode()

    override fun toString(): String = "Split(splitId=$splitId, name=$name)"
}
