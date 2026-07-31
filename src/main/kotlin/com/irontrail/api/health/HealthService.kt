package com.irontrail.api.health

import org.springframework.stereotype.Service

enum class HealthStatus { UP, DOWN }

@Service
class HealthService {

    fun status(): HealthStatus = HealthStatus.UP
}