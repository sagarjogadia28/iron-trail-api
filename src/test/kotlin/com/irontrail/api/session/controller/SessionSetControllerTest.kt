package com.irontrail.api.session.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.session.dto.SessionSetPatchRequest
import com.irontrail.api.session.dto.SessionSetResponse
import com.irontrail.api.session.service.WorkoutSessionService
import com.irontrail.api.split.model.SetType
import com.irontrail.api.testsupport.authenticateAs
import com.irontrail.api.testsupport.clearAuthentication
import com.irontrail.api.testsupport.standaloneMvcBuilder
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SessionSetControllerTest {
    private val workoutSessionService: WorkoutSessionService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(SessionSetController(workoutSessionService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    // ---- update (this is the actual set-logging endpoint) ----

    @Test
    fun `PATCH logs a completed set and returns 200`() {
        val request = SessionSetPatchRequest(reps = 8, weightKg = 60.0, isCompleted = true)
        whenever(workoutSessionService.updateSessionSet(eq(1L), eq(request), eq(10L))).thenReturn(
            SessionSetResponse(1L, 0, SetType.NORMAL, 8, null, null, 8, 60.0, null, true),
        )

        mockMvc
            .perform(
                patch("/v1/session-sets/1")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"reps":8,"weightKg":60.0,"isCompleted":true}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.isCompleted").value(true))
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(workoutSessionService.updateSessionSet(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("SessionSet", 1L))

        mockMvc
            .perform(patch("/v1/session-sets/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/session-sets/1")).andExpect(status().isNoContent)

        verify(workoutSessionService).deleteSessionSet(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("SessionSet", 1L)).whenever(workoutSessionService).deleteSessionSet(1L, 10L)

        mockMvc.perform(delete("/v1/session-sets/1")).andExpect(status().isNotFound)
    }
}
