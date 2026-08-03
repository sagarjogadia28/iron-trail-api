package com.irontrail.api.session.repository

import com.irontrail.api.session.model.SessionSet
import org.springframework.data.jpa.repository.JpaRepository

interface SessionSetRepository : JpaRepository<SessionSet, Long>
