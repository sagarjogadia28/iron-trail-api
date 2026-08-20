package com.irontrail.api.user.controller

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.testsupport.authenticateAs
import com.irontrail.api.testsupport.clearAuthentication
import com.irontrail.api.testsupport.standaloneMvcBuilder
import com.irontrail.api.user.dto.UserProfilePatchRequest
import com.irontrail.api.user.dto.UserProfileRequest
import com.irontrail.api.user.dto.UserProfileResponse
import com.irontrail.api.user.model.Gender
import com.irontrail.api.user.model.MeasurementUnit
import com.irontrail.api.user.model.WeightUnit
import com.irontrail.api.user.service.UserProfileService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

class UserProfileControllerTest {
    private val userProfileService: UserProfileService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(UserProfileController(userProfileService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    private fun response(name: String = "Sagar") =
        UserProfileResponse(
            userId = 10L,
            name = name,
            gender = Gender.MALE,
            weightUnit = WeightUnit.KG,
            measurementUnit = MeasurementUnit.METRIC,
            restTimerNotificationsEnabled = true,
            activeSplitId = null,
            createdAt = OffsetDateTime.now(),
        )

    // ---- findByUserId ----

    @Test
    fun `GET returns 200 with the caller's own profile - no id in the route`() {
        whenever(userProfileService.findByUserId(10L)).thenReturn(response())

        mockMvc
            .perform(get("/v1/profile"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Sagar"))
    }

    @Test
    fun `GET returns 404 before onboarding creates a profile`() {
        whenever(userProfileService.findByUserId(10L)).thenThrow(NotFoundException("UserProfile", 10L))

        mockMvc.perform(get("/v1/profile")).andExpect(status().isNotFound)
    }

    // ---- create ----

    @Test
    fun `POST with a valid body returns 201`() {
        val request = UserProfileRequest("Sagar", Gender.MALE, WeightUnit.KG, MeasurementUnit.METRIC)
        whenever(userProfileService.create(request, 10L)).thenReturn(response())

        mockMvc
            .perform(
                post("/v1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Sagar","gender":"MALE","weightUnit":"KG","measurementUnit":"METRIC"}"""),
            ).andExpect(status().isCreated)
    }

    @Test
    fun `POST retried after a profile already exists returns 409, not a duplicate create`() {
        whenever(userProfileService.create(any(), eq(10L))).thenThrow(ConflictException("Profile already exists"))

        mockMvc
            .perform(
                post("/v1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Sagar","gender":"MALE","weightUnit":"KG","measurementUnit":"METRIC"}"""),
            ).andExpect(status().isConflict)
    }

    @Test
    fun `POST with a name under 2 characters returns 400 and never calls the service`() {
        mockMvc
            .perform(
                post("/v1/profile")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"S","gender":"MALE","weightUnit":"KG","measurementUnit":"METRIC"}"""),
            ).andExpect(status().isBadRequest)

        verify(userProfileService, never()).create(any(), any())
    }

    // ---- update ----

    @Test
    fun `PATCH with a partial body returns 200 with the patched profile`() {
        whenever(userProfileService.update(eq(UserProfilePatchRequest(weightUnit = WeightUnit.LBS)), eq(10L)))
            .thenReturn(response())

        mockMvc
            .perform(patch("/v1/profile").contentType(MediaType.APPLICATION_JSON).content("""{"weightUnit":"LBS"}"""))
            .andExpect(status().isOk)
    }

    @Test
    fun `PATCH setting activeSplitId to a split the caller doesn't own returns 404`() {
        whenever(userProfileService.update(any(), eq(10L))).thenThrow(NotFoundException("Split", 99L))

        mockMvc
            .perform(patch("/v1/profile").contentType(MediaType.APPLICATION_JSON).content("""{"activeSplitId":99}"""))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `PATCH with a blank name returns 400 and never calls the service`() {
        mockMvc
            .perform(patch("/v1/profile").contentType(MediaType.APPLICATION_JSON).content("""{"name":"   "}"""))
            .andExpect(status().isBadRequest)

        verify(userProfileService, never()).update(any(), any())
    }
}
