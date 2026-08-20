package com.irontrail.api.split.controller

import com.irontrail.api.common.BadRequestException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.dto.TemplateSetPatchRequest
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

class TemplateSetControllerTest {
    private val splitService: SplitService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(TemplateSetController(splitService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    // ---- update ----

    @Test
    fun `PATCH returns 200 with the patched set`() {
        whenever(splitService.updateTemplateSet(eq(1L), eq(TemplateSetPatchRequest(setType = SetType.WARMUP)), eq(10L)))
            .thenReturn(TemplateSetResponse(1L, 0, 8, null, null, SetType.WARMUP))

        mockMvc
            .perform(
                patch("/v1/template-sets/1").contentType(MediaType.APPLICATION_JSON).content("""{"setType":"WARMUP"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.setType").value("WARMUP"))
    }

    @Test
    fun `PATCH with an invalid rep range returns 400`() {
        whenever(splitService.updateTemplateSet(eq(1L), any(), eq(10L)))
            .thenThrow(BadRequestException("targetReps must not exceed targetRepsMax"))

        mockMvc
            .perform(
                patch("/v1/template-sets/1").contentType(MediaType.APPLICATION_JSON).content("""{"targetReps":20}"""),
            ).andExpect(status().isBadRequest)
    }

    @Test
    fun `PATCH returns 404 when not owned by the caller`() {
        whenever(splitService.updateTemplateSet(eq(1L), any(), eq(10L))).thenThrow(NotFoundException("TemplateSet", 1L))

        mockMvc
            .perform(patch("/v1/template-sets/1").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the caller's id`() {
        mockMvc.perform(delete("/v1/template-sets/1")).andExpect(status().isNoContent)

        verify(splitService).deleteTemplateSet(1L, 10L)
    }

    @Test
    fun `DELETE returns 404 when not owned by the caller`() {
        doThrow(NotFoundException("TemplateSet", 1L)).whenever(splitService).deleteTemplateSet(1L, 10L)

        mockMvc.perform(delete("/v1/template-sets/1")).andExpect(status().isNotFound)
    }
}
