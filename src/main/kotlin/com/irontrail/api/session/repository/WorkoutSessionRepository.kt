package com.irontrail.api.session.repository

import com.irontrail.api.session.model.WorkoutSession
import org.springframework.data.jpa.repository.JpaRepository

interface WorkoutSessionRepository : JpaRepository<WorkoutSession, Long>