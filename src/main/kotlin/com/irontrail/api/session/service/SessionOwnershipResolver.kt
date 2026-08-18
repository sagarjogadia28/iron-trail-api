package com.irontrail.api.session.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.session.model.SessionExercise
import com.irontrail.api.session.model.SessionSet
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.SessionExerciseRepository
import com.irontrail.api.session.repository.SessionSetRepository
import com.irontrail.api.session.repository.WorkoutSessionRepository
import org.springframework.stereotype.Component

@Component
class SessionOwnershipResolver(
    private val workoutSessionRepository: WorkoutSessionRepository,
    private val sessionExerciseRepository: SessionExerciseRepository,
    private val sessionSetRepository: SessionSetRepository
) {
    fun getOwnedWorkoutSession(sessionId: Long, userId: Long): WorkoutSession =
        workoutSessionRepository.findBySessionIdAndOwnerId(sessionId, userId)
            ?: throw NotFoundException("WorkoutSession", sessionId)

    fun getOwnedSessionExercise(sessionExerciseId: Long, userId: Long): SessionExercise =
        sessionExerciseRepository.findOwnedBySessionExerciseId(sessionExerciseId, userId)
            ?: throw NotFoundException("SessionExercise", sessionExerciseId)

    fun getOwnedSessionSet(sessionSetId: Long, userId: Long): SessionSet =
        sessionSetRepository.findOwnedBySessionSetId(sessionSetId, userId)
            ?: throw NotFoundException("SessionSet", sessionSetId)
}
