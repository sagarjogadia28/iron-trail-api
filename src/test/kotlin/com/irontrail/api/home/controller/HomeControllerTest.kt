package com.irontrail.api.home.controller

import com.irontrail.api.home.dto.HomeResponse
import com.irontrail.api.home.dto.NextWorkoutResponse
import com.irontrail.api.home.service.HomeService
import com.irontrail.api.testsupport.authenticateAs
import com.irontrail.api.testsupport.clearAuthentication
import com.irontrail.api.testsupport.standaloneMvcBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class HomeControllerTest {

    private val homeService: HomeService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(HomeController(homeService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    @Test
    fun `GET returns 200 with the caller's dashboard`() {
        val response = HomeResponse(
            nextWorkout = NextWorkoutResponse(1L, "Push Day", "PPL", 5),
            trainedDatesThisMonth = emptyList(),
            workoutsThisMonth = 3,
            weekStreak = 2,
            recentWorkouts = emptyList()
        )
        whenever(homeService.getHome(10L)).thenReturn(response)

        mockMvc.perform(get("/v1/home"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nextWorkout.workoutDayName").value("Push Day"))
            .andExpect(jsonPath("$.weekStreak").value(2))
    }

    @Test
    fun `GET returns nextWorkout null and empty collections for a user with no split yet`() {
        whenever(homeService.getHome(10L)).thenReturn(HomeResponse(null, emptyList(), 0, 0, emptyList()))

        mockMvc.perform(get("/v1/home"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.nextWorkout").doesNotExist())
    }
}
