package com.irontrail.api.user.repository

import com.irontrail.api.user.model.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long>