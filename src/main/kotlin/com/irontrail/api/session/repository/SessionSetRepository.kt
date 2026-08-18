package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SessionSetRepository : JpaRepository<SessionSet, Long> {
    fun findBySessionExerciseIn(sessionExercises: List<SessionExercise>): List<SessionSet>

    @Query("SELECT ss FROM SessionSet ss WHERE ss.sessionSetId = :sessionSetId AND ss.sessionExercise.workoutSession.ownerId = :ownerId")
    fun findOwnedBySessionSetId(
        @Param("sessionSetId") sessionSetId: Long,
        @Param("ownerId") ownerId: Long
    ): SessionSet?
}