package com.irontrail.api.session.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.session.dto.SessionExercisePatchRequest
import com.irontrail.api.session.dto.SessionExerciseResponse
import com.irontrail.api.session.dto.SessionSetRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SessionExerciseControllerTest {
    private val workoutSessionService: WorkoutSessionService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(SessionExerciseController(workoutSessionService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    private fun setResponse(
        id: Long = 1L,
        reps: Int? = 8,
    ) = SessionSetResponse(
        sessionSetId = id,
        sortOrder = 0,
        setType = SetType.NORMAL,
        targetReps = 8,
        targetRepsMax = null,
        targetDurationSeconds = null,
        reps = reps,
        weightKg = null,
        durationSeconds = null,
        isCompleted = reps != null,
    )

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the patched exercise`() {
        whenever(workoutSessionService.updateSessionExercise(eq(1L), eq(SessionExercisePatchRequest(notes = "updated")), eq(10L)))
            .thenReturn(SessionExerciseResponse(1L, 500L, "Bench Press", ExerciseInputType.REPS, true, 90, 0, "updated", emptyList()))

        mockMvc
            .perform(
                patch("/v1/session-exercises/1").contentType(MediaType.APPLICATION_JSON).content("""{"notes":"updated"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").value("updated"))
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(workoutSessionService.updateSessionExercise(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("SessionExercise", 1L))

        mockMvc
            .perform(patch("/v1/session-exercises/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/session-exercises/1")).andExpect(status().isNoContent)

        verify(workoutSessionService).deleteSessionExercise(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("SessionExercise", 1L)).whenever(workoutSessionService).deleteSessionExercise(1L, 10L)

        mockMvc.perform(delete("/v1/session-exercises/1")).andExpect(status().isNotFound)
    }

    // ---- findPreviousPerformance ----

    @Test
    fun `GET previous-performance returns 200 with the prior sets`() {
        whenever(workoutSessionService.findPreviousPerformance(1L, 10L)).thenReturn(listOf(setResponse()))

        mockMvc
            .perform(get("/v1/session-exercises/1/previous-performance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].reps").value(8))
    }

    @Test
    fun `GET previous-performance returns 200 with an empty list when there's no prior data`() {
        whenever(workoutSessionService.findPreviousPerformance(1L, 10L)).thenReturn(emptyList())

        mockMvc
            .perform(get("/v1/session-exercises/1/previous-performance"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$").isArray)
            .andExpect(jsonPath("$").isEmpty)
    }

    // ---- createSessionSet ----

    @Test
    fun `POST session-sets returns 201 under the given exercise`() {
        val request = SessionSetRequest(sortOrder = 0, setType = SetType.NORMAL, targetReps = 8)
        whenever(workoutSessionService.createSessionSet(eq(1L), eq(request), eq(10L))).thenReturn(setResponse(id = 10L, reps = null))

        mockMvc
            .perform(
                post("/v1/session-exercises/1/session-sets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sortOrder":0,"setType":"NORMAL","targetReps":8}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.sessionSetId").value(10))
    }

    @Test
    fun `POST session-sets returns 404 when the parent exercise isn't owned by the caller`() {
        whenever(workoutSessionService.createSessionSet(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("SessionExercise", 1L))

        mockMvc
            .perform(
                post("/v1/session-exercises/1/session-sets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sortOrder":0,"setType":"NORMAL"}"""),
            ).andExpect(status().isNotFound)
    }
}
