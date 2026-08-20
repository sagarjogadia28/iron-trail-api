package com.irontrail.api.user.repository

import com.irontrail.api.user.model.UserProfile
import org.springframework.data.jpa.repository.JpaRepository

interface UserProfileRepository : JpaRepository<UserProfile, Long>
