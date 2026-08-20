package com.irontrail.api.split.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.dto.SplitDetailResponse
import com.irontrail.api.split.dto.SplitPatchRequest
import com.irontrail.api.split.dto.SplitRequest
import com.irontrail.api.split.dto.SplitResponse
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class SplitControllerTest {
    private val splitService: SplitService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(SplitController(splitService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    private fun detail(
        id: Long = 1L,
        name: String = "PPL",
    ) = SplitDetailResponse(id, name, emptyList())

    // ---- findAll ----

    @Test
    fun `GET list returns 200 with the caller's splits`() {
        whenever(splitService.findAll(10L)).thenReturn(listOf(SplitResponse(1L, "PPL", 3, 12)))

        mockMvc
            .perform(get("/v1/splits"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("PPL"))
    }

    // ---- findById ----

    @Test
    fun `GET by id returns 200 with the nested tree`() {
        whenever(splitService.findById(1L, 10L)).thenReturn(detail())

        mockMvc
            .perform(get("/v1/splits/1"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.splitId").value(1))
    }

    @Test
    fun `GET by id returns 404 when not owned by the caller`() {
        whenever(splitService.findById(1L, 10L)).thenThrow(NotFoundException("Split", 1L))

        mockMvc.perform(get("/v1/splits/1")).andExpect(status().isNotFound)
    }

    // ---- create ----

    @Test
    fun `POST with a valid body returns 201`() {
        whenever(splitService.create(SplitRequest("New Split"), 10L)).thenReturn(detail(name = "New Split"))

        mockMvc
            .perform(
                post("/v1/splits").contentType(MediaType.APPLICATION_JSON).content("""{"name":"New Split"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("New Split"))
    }

    @Test
    fun `POST with a blank name returns 400 and never calls the service`() {
        mockMvc
            .perform(post("/v1/splits").contentType(MediaType.APPLICATION_JSON).content("""{"name":""}"""))
            .andExpect(status().isBadRequest)

        verify(splitService, never()).create(any(), any())
    }

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the patched split`() {
        whenever(splitService.update(eq(1L), eq(SplitPatchRequest(name = "Renamed")), eq(10L)))
            .thenReturn(detail(name = "Renamed"))

        mockMvc
            .perform(patch("/v1/splits/1").contentType(MediaType.APPLICATION_JSON).content("""{"name":"Renamed"}"""))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed"))
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(splitService.update(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("Split", 1L))

        mockMvc
            .perform(patch("/v1/splits/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/splits/1")).andExpect(status().isNoContent)

        verify(splitService).delete(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        org.mockito.kotlin
            .doThrow(NotFoundException("Split", 1L))
            .whenever(splitService)
            .delete(1L, 10L)

        mockMvc.perform(delete("/v1/splits/1")).andExpect(status().isNotFound)
    }

    // ---- duplicate ----

    @Test
    fun `POST duplicate returns 201 with the deep-copied split`() {
        whenever(splitService.duplicateSplit(eq(1L), eq(SplitRequest("PPL (Copy)")), eq(10L)))
            .thenReturn(detail(id = 2L, name = "PPL (Copy)"))

        mockMvc
            .perform(
                post("/v1/splits/1/duplicate")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"PPL (Copy)"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.splitId").value(2))
    }

    @Test
    fun `POST duplicate returns 404 when the source split isn't owned by the caller`() {
        whenever(splitService.duplicateSplit(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("Split", 1L))

        mockMvc
            .perform(
                post("/v1/splits/1/duplicate").contentType(MediaType.APPLICATION_JSON).content("""{"name":"Copy"}"""),
            ).andExpect(status().isNotFound)
    }

    // ---- createWorkoutDay ----

    @Test
    fun `POST workout-days returns 201 under the given split`() {
        whenever(splitService.createWorkoutDay(eq(1L), eq(WorkoutDayRequest("Push", 0)), eq(10L)))
            .thenReturn(WorkoutDayResponse(10L, "Push", 0, emptyList()))

        mockMvc
            .perform(
                post("/v1/splits/1/workout-days")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Push","sortOrder":0}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.workoutDayId").value(10))
    }

    @Test
    fun `POST workout-days with a blank name returns 400 and never calls the service`() {
        mockMvc
            .perform(
                post("/v1/splits/1/workout-days")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"","sortOrder":0}"""),
            ).andExpect(status().isBadRequest)

        verify(splitService, never()).createWorkoutDay(any(), any(), any())
    }

    @Test
    fun `POST workout-days returns 404 when the parent split isn't owned by the caller`() {
        whenever(splitService.createWorkoutDay(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("Split", 1L))

        mockMvc
            .perform(
                post("/v1/splits/1/workout-days")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"Push","sortOrder":0}"""),
            ).andExpect(status().isNotFound)
    }
}
