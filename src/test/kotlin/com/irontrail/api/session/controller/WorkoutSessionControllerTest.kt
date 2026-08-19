package com.irontrail.api.session.controller

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.session.dto.SessionExerciseRequest
import com.irontrail.api.session.dto.SessionExerciseResponse
import com.irontrail.api.session.dto.WorkoutSessionDetailResponse
import com.irontrail.api.session.dto.WorkoutSessionPatchRequest
import com.irontrail.api.session.dto.WorkoutSessionRequest
import com.irontrail.api.session.dto.WorkoutSessionResponse
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.service.WorkoutSessionService
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
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.OffsetDateTime

class WorkoutSessionControllerTest {

    private val workoutSessionService: WorkoutSessionService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(WorkoutSessionController(workoutSessionService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    private fun sessionResponse(id: Long = 1L, status: SessionStatus = SessionStatus.ACTIVE) = WorkoutSessionResponse(
        sessionId = id,
        workoutDayId = null,
        splitNameSnapshot = null,
        workoutDayNameSnapshot = null,
        startedAt = OffsetDateTime.now(),
        durationSeconds = 0,
        totalVolumeKg = null,
        completedSets = null,
        totalSets = null,
        notes = null,
        status = status
    )

    private fun detailResponse(id: Long = 1L, status: SessionStatus = SessionStatus.ACTIVE) =
        WorkoutSessionDetailResponse(sessionResponse(id, status), emptyList())

    // ---- findAll ----

    @Test
    fun `GET list returns 200 with the caller's sessions`() {
        whenever(workoutSessionService.findAll(10L, null)).thenReturn(listOf(sessionResponse()))

        mockMvc.perform(get("/v1/workout-sessions"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].sessionId").value(1))
    }

    @Test
    fun `GET list forwards the splitName filter`() {
        whenever(workoutSessionService.findAll(10L, "PPL")).thenReturn(emptyList())

        mockMvc.perform(get("/v1/workout-sessions").param("splitName", "PPL")).andExpect(status().isOk)

        verify(workoutSessionService).findAll(10L, "PPL")
    }

    // ---- findActive ----

    @Test
    fun `GET active returns 200 with the active session when one exists`() {
        whenever(workoutSessionService.findActive(10L)).thenReturn(sessionResponse(status = SessionStatus.ACTIVE))

        mockMvc.perform(get("/v1/workout-sessions/active")).andExpect(status().isOk)
    }

    @Test
    fun `GET active returns 204 with no body when there's no active session`() {
        whenever(workoutSessionService.findActive(10L)).thenReturn(null)

        mockMvc.perform(get("/v1/workout-sessions/active"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))
    }

    // ---- findById ----

    @Test
    fun `GET by id returns 200 with the nested detail`() {
        whenever(workoutSessionService.findById(1L, 10L)).thenReturn(detailResponse())

        mockMvc.perform(get("/v1/workout-sessions/1")).andExpect(status().isOk)
    }

    @Test
    fun `GET by id returns 404 when not owned by the caller`() {
        whenever(workoutSessionService.findById(1L, 10L)).thenThrow(NotFoundException("WorkoutSession", 1L))

        mockMvc.perform(get("/v1/workout-sessions/1")).andExpect(status().isNotFound)
    }

    // ---- create ----

    @Test
    fun `POST with a valid body returns 201`() {
        whenever(workoutSessionService.create(eq(WorkoutSessionRequest(workoutDayId = 5L)), eq(10L)))
            .thenReturn(detailResponse())

        mockMvc.perform(
            post("/v1/workout-sessions").contentType(MediaType.APPLICATION_JSON).content("""{"workoutDayId":5}""")
        ).andExpect(status().isCreated)
    }

    @Test
    fun `POST returns 409 when the caller already has an active session`() {
        whenever(workoutSessionService.create(any(), eq(10L))).thenThrow(ConflictException("Active session already exists"))

        mockMvc.perform(post("/v1/workout-sessions").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isConflict)
    }

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the updated session`() {
        whenever(workoutSessionService.update(eq(1L), eq(WorkoutSessionPatchRequest(status = SessionStatus.COMPLETED)), eq(10L)))
            .thenReturn(detailResponse(status = SessionStatus.COMPLETED))

        mockMvc.perform(
            patch("/v1/workout-sessions/1").contentType(MediaType.APPLICATION_JSON).content("""{"status":"COMPLETED"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `PATCH attempting to reopen a completed session returns 409`() {
        whenever(workoutSessionService.update(eq(1L), any(), eq(10L))).thenThrow(ConflictException("Session is COMPLETED and cannot be reopened"))

        mockMvc.perform(
            patch("/v1/workout-sessions/1").contentType(MediaType.APPLICATION_JSON).content("""{"status":"ACTIVE"}""")
        ).andExpect(status().isConflict)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/workout-sessions/1")).andExpect(status().isNoContent)

        verify(workoutSessionService).delete(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("WorkoutSession", 1L)).whenever(workoutSessionService).delete(1L, 10L)

        mockMvc.perform(delete("/v1/workout-sessions/1")).andExpect(status().isNotFound)
    }

    // ---- createSessionExercise ----

    @Test
    fun `POST session-exercises returns 201 under the given session`() {
        val request = SessionExerciseRequest(exerciseId = 500L, isRepRange = true, restDurationSeconds = 90, sortOrder = 0)
        whenever(workoutSessionService.createSessionExercise(eq(1L), eq(request), eq(10L))).thenReturn(
            SessionExerciseResponse(100L, 500L, "Bench Press", com.irontrail.api.exercise.model.ExerciseInputType.REPS, true, 90, 0, null, emptyList())
        )

        mockMvc.perform(
            post("/v1/workout-sessions/1/session-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"exerciseId":500,"isRepRange":true,"restDurationSeconds":90,"sortOrder":0}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.sessionExerciseId").value(100))
    }

    @Test
    fun `POST session-exercises returns 404 when the parent session isn't owned by the caller`() {
        whenever(workoutSessionService.createSessionExercise(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("WorkoutSession", 1L))

        mockMvc.perform(
            post("/v1/workout-sessions/1/session-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"exerciseId":500,"isRepRange":true,"restDurationSeconds":90,"sortOrder":0}""")
        ).andExpect(status().isNotFound)
    }
}
