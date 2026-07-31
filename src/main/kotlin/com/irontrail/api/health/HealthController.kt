package com.irontrail.api.health

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/health")
class HealthController(
    private val healthService: HealthService
) {
    @GetMapping
    fun health(): HealthStatus = healthService.status()
}