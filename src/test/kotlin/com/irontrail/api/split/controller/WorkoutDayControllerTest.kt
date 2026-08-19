package com.irontrail.api.split.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.dto.TemplateExerciseRequest
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.WorkoutDayPatchRequest
import com.irontrail.api.split.dto.WorkoutDayRequest
import com.irontrail.api.split.dto.WorkoutDayResponse
import com.irontrail.api.split.service.SplitService
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class WorkoutDayControllerTest {

    private val splitService: SplitService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(WorkoutDayController(splitService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the patched day`() {
        whenever(splitService.updateWorkoutDay(eq(1L), eq(WorkoutDayPatchRequest(name = "Renamed")), eq(10L)))
            .thenReturn(WorkoutDayResponse(1L, "Renamed", 0, emptyList()))

        mockMvc.perform(patch("/v1/workout-days/1").contentType(MediaType.APPLICATION_JSON).content("""{"name":"Renamed"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed"))
    }

    @Test
    fun `PATCH with a blank name returns 400 and never calls the service`() {
        mockMvc.perform(patch("/v1/workout-days/1").contentType(MediaType.APPLICATION_JSON).content("""{"name":"   "}"""))
            .andExpect(status().isBadRequest)

        verify(splitService, never()).updateWorkoutDay(any(), any(), any())
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(splitService.updateWorkoutDay(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("WorkoutDay", 1L))

        mockMvc.perform(patch("/v1/workout-days/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/workout-days/1")).andExpect(status().isNoContent)

        verify(splitService).deleteWorkoutDay(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("WorkoutDay", 1L)).whenever(splitService).deleteWorkoutDay(1L, 10L)

        mockMvc.perform(delete("/v1/workout-days/1")).andExpect(status().isNotFound)
    }

    // ---- duplicate ----

    @Test
    fun `POST duplicate returns 201, landing in the same split as the source day`() {
        whenever(splitService.duplicateWorkoutDay(eq(1L), eq(WorkoutDayRequest("Push (Copy)", 1)), eq(10L)))
            .thenReturn(WorkoutDayResponse(2L, "Push (Copy)", 1, emptyList()))

        mockMvc.perform(
            post("/v1/workout-days/1/duplicate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Push (Copy)","sortOrder":1}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.workoutDayId").value(2))
    }

    @Test
    fun `POST duplicate returns 404 when the source day isn't owned by the caller`() {
        whenever(splitService.duplicateWorkoutDay(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("WorkoutDay", 1L))

        mockMvc.perform(
            post("/v1/workout-days/1/duplicate")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"name":"Copy","sortOrder":0}""")
        ).andExpect(status().isNotFound)
    }

    // ---- createTemplateExercise ----

    @Test
    fun `POST template-exercises returns 201 under the given day`() {
        val request = TemplateExerciseRequest(exerciseId = 500L, sortOrder = 0)
        whenever(splitService.createTemplateExercise(eq(1L), eq(request), eq(10L)))
            .thenReturn(TemplateExerciseResponse(100L, 500L, 0, 90, true, null, emptyList()))

        mockMvc.perform(
            post("/v1/workout-days/1/template-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"exerciseId":500,"sortOrder":0}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateExerciseId").value(100))
    }

    @Test
    fun `POST template-exercises returns 404 when the referenced exercise isn't visible to the caller`() {
        whenever(splitService.createTemplateExercise(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("Exercise", 500L))

        mockMvc.perform(
            post("/v1/workout-days/1/template-exercises")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"exerciseId":500,"sortOrder":0}""")
        ).andExpect(status().isNotFound)
    }
}
