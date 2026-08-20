package com.irontrail.api.split.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.dto.TemplateExercisePatchRequest
import com.irontrail.api.split.dto.TemplateExerciseResponse
import com.irontrail.api.split.dto.TemplateSetRequest
import com.irontrail.api.split.dto.TemplateSetResponse
import com.irontrail.api.split.model.SetType
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
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TemplateExerciseControllerTest {
    private val splitService: SplitService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(TemplateExerciseController(splitService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the patched exercise`() {
        whenever(splitService.updateTemplateExercise(eq(1L), eq(TemplateExercisePatchRequest(notes = "slow eccentric")), eq(10L)))
            .thenReturn(TemplateExerciseResponse(1L, 500L, 0, 90, true, "slow eccentric", emptyList()))

        mockMvc
            .perform(
                patch("/v1/template-exercises/1").contentType(MediaType.APPLICATION_JSON).content("""{"notes":"slow eccentric"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.notes").value("slow eccentric"))
    }

    @Test
    fun `PATCH cannot change exerciseId - the request body has no such field`() {
        whenever(splitService.updateTemplateExercise(eq(1L), eq(TemplateExercisePatchRequest()), eq(10L)))
            .thenReturn(TemplateExerciseResponse(1L, 500L, 0, 90, true, null, emptyList()))

        // Jackson silently drops unknown-to-the-DTO fields like exerciseId, so this must reach the
        // service as an empty patch, not one carrying exerciseId=999.
        mockMvc
            .perform(
                patch("/v1/template-exercises/1").contentType(MediaType.APPLICATION_JSON).content("""{"exerciseId":999}"""),
            ).andExpect(status().isOk)

        verify(splitService).updateTemplateExercise(1L, TemplateExercisePatchRequest(), 10L)
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(splitService.updateTemplateExercise(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("TemplateExercise", 1L))

        mockMvc
            .perform(patch("/v1/template-exercises/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/template-exercises/1")).andExpect(status().isNoContent)

        verify(splitService).deleteTemplateExercise(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("TemplateExercise", 1L)).whenever(splitService).deleteTemplateExercise(1L, 10L)

        mockMvc.perform(delete("/v1/template-exercises/1")).andExpect(status().isNotFound)
    }

    // ---- createTemplateSet ----

    @Test
    fun `POST template-sets returns 201 under the given exercise`() {
        val request = TemplateSetRequest(sortOrder = 0, targetReps = 8, setType = SetType.NORMAL)
        whenever(splitService.createTemplateSet(eq(1L), eq(request), eq(10L)))
            .thenReturn(TemplateSetResponse(10L, 0, 8, null, null, SetType.NORMAL))

        mockMvc
            .perform(
                post("/v1/template-exercises/1/template-sets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sortOrder":0,"targetReps":8,"setType":"NORMAL"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.templateSetId").value(10))
    }

    @Test
    fun `POST template-sets with targetReps greater than targetRepsMax returns 400`() {
        whenever(splitService.createTemplateSet(eq(1L), any(), eq(10L)))
            .thenThrow(
                com.irontrail.api.common
                    .BadRequestException("targetReps must not exceed targetRepsMax"),
            )

        mockMvc
            .perform(
                post("/v1/template-exercises/1/template-sets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sortOrder":0,"targetReps":12,"targetRepsMax":8,"setType":"NORMAL"}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `POST template-sets returns 404 when the parent exercise isn't owned by the caller`() {
        whenever(splitService.createTemplateSet(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("TemplateExercise", 1L))

        mockMvc
            .perform(
                post("/v1/template-exercises/1/template-sets")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"sortOrder":0,"setType":"NORMAL"}"""),
            ).andExpect(status().isNotFound)
    }
}
