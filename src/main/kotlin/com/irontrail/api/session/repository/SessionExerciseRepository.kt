package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionExercise
import org.springframework.data.jpa.repository.JpaRepository

interface SessionExerciseRepository : JpaRepository<SessionExercise, Long>