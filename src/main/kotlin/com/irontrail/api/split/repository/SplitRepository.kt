package com.irontrail.api.split.repository

import com.irontrail.api.split.model.Split
import org.springframework.data.jpa.repository.JpaRepository

interface SplitRepository : JpaRepository<Split, Long> {
    fun findByOwnerId(ownerId: Long) : List<Split>
    fun findBySplitIdAndOwnerId(splitId: Long, ownerId: Long) : Split?
}