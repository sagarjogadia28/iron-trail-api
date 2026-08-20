package com.irontrail.api.home.controller

import com.irontrail.api.home.dto.HomeResponse
import com.irontrail.api.home.service.HomeService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/v1/home")
class HomeController(
    private val homeService: HomeService,
) {
    @GetMapping
    fun getHome(
        @AuthenticationPrincipal userId: Long,
    ): HomeResponse = homeService.getHome(userId)
}
