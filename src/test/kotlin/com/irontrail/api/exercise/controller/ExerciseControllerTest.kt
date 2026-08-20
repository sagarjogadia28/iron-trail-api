package com.irontrail.api.exercise.controller

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.dto.ExercisePatchRequest
import com.irontrail.api.exercise.dto.ExerciseRequest
import com.irontrail.api.exercise.dto.ExerciseResponse
import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.exercise.service.ExerciseService
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

class ExerciseControllerTest {
    private val exerciseService: ExerciseService = mock()
    private val mockMvc: MockMvc = standaloneMvcBuilder(ExerciseController(exerciseService)).build()

    @BeforeEach
    fun authenticate() = authenticateAs(10L)

    @AfterEach
    fun clearAuth() = clearAuthentication()

    private fun response(
        id: Long = 5L,
        name: String = "Bench Press",
        ownerId: Long? = null,
    ) = ExerciseResponse(
        exerciseId = id,
        wgerId = null,
        name = name,
        primaryMuscleGroup = MuscleGroup.CHEST,
        secondaryMuscleGroups = emptyList(),
        equipment = Equipment.BARBELL,
        inputType = ExerciseInputType.REPS,
        description = null,
        imageUrl = null,
        ownerId = ownerId,
    )

    // ---- findAll ----

    @Test
    fun `GET list returns 200 with the mapped body`() {
        whenever(exerciseService.findAll(null, null, 10L)).thenReturn(listOf(response()))

        mockMvc
            .perform(get("/v1/exercises"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].name").value("Bench Press"))
    }

    @Test
    fun `GET list forwards search and muscleGroups query params, and the caller's own id`() {
        whenever(exerciseService.findAll(any(), any(), any())).thenReturn(emptyList())

        mockMvc
            .perform(
                get("/v1/exercises")
                    .param("search", "bench")
                    .param("muscleGroups", "CHEST", "TRICEPS"),
            ).andExpect(status().isOk)

        verify(exerciseService).findAll("bench", listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS), 10L)
    }

    @Test
    fun `GET list with an invalid muscleGroups enum value returns 400 with the standard error shape`() {
        mockMvc
            .perform(get("/v1/exercises").param("muscleGroups", "NOT_A_REAL_GROUP"))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Invalid value for parameter 'muscleGroups'"))
    }

    // ---- findById ----

    @Test
    fun `GET by id returns 200 with the mapped body`() {
        whenever(exerciseService.findById(5L, 10L)).thenReturn(response(id = 5L))

        mockMvc
            .perform(get("/v1/exercises/5"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.exerciseId").value(5))
    }

    @Test
    fun `GET by id returns 404 with the standard error shape when the service throws NotFoundException`() {
        whenever(exerciseService.findById(999L, 10L)).thenThrow(NotFoundException("Exercise", 999L))

        mockMvc
            .perform(get("/v1/exercises/999"))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.message").value("Exercise not found: 999"))
    }

    // ---- create ----

    @Test
    fun `POST with a valid body returns 201 with the created resource`() {
        val json =
            """
            {"name":"Incline Press","primaryMuscleGroup":"CHEST","secondaryMuscleGroups":[],
             "equipment":"DUMBBELL","inputType":"REPS","description":null}
            """.trimIndent()
        whenever(exerciseService.create(any(), eq(10L))).thenReturn(response(id = 42L, name = "Incline Press"))

        mockMvc
            .perform(
                post("/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(json),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.exerciseId").value(42))

        verify(exerciseService).create(
            ExerciseRequest(
                name = "Incline Press",
                primaryMuscleGroup = MuscleGroup.CHEST,
                secondaryMuscleGroups = emptyList(),
                equipment = Equipment.DUMBBELL,
                inputType = ExerciseInputType.REPS,
                description = null,
            ),
            10L,
        )
    }

    @Test
    fun `POST with a blank name returns 400 with field-level validation errors, and never calls the service`() {
        val invalidJson = """{"name":"","primaryMuscleGroup":"CHEST","equipment":"BARBELL","inputType":"REPS"}"""

        mockMvc
            .perform(post("/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(invalidJson))
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.name").exists())

        verify(exerciseService, never()).create(any(), any())
    }

    @Test
    fun `POST with a missing required field returns 400 via the malformed-body handler, not a 500`() {
        val missingFieldsJson = """{"name":"Incline Press"}"""

        mockMvc
            .perform(post("/v1/exercises").contentType(MediaType.APPLICATION_JSON).content(missingFieldsJson))
            .andExpect(status().isBadRequest)
    }

    // ---- update ----

    @Test
    fun `PATCH with a partial body returns 200 with the patched resource`() {
        val json = """{"name":"Renamed"}"""
        whenever(exerciseService.update(eq(5L), eq(ExercisePatchRequest(name = "Renamed")), eq(10L)))
            .thenReturn(response(id = 5L, name = "Renamed"))

        mockMvc
            .perform(patch("/v1/exercises/5").contentType(MediaType.APPLICATION_JSON).content(json))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Renamed"))
    }

    @Test
    fun `PATCH with a blank name returns 400 and never calls the service`() {
        val blankNameJson = """{"name":"   "}"""

        mockMvc
            .perform(patch("/v1/exercises/5").contentType(MediaType.APPLICATION_JSON).content(blankNameJson))
            .andExpect(status().isBadRequest)

        verify(exerciseService, never()).update(any(), any(), any())
    }

    @Test
    fun `PATCH returns 404 when the service reports the exercise isn't owned by the caller`() {
        whenever(exerciseService.update(eq(5L), any(), eq(10L))).thenThrow(NotFoundException("Exercise", 5L))

        mockMvc
            .perform(patch("/v1/exercises/5").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isNotFound)
    }

    // ---- delete ----

    @Test
    fun `DELETE returns 204 and calls the service with the path id and caller's id`() {
        mockMvc
            .perform(delete("/v1/exercises/5"))
            .andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(exerciseService).delete(5L, 10L)
    }

    @Test
    fun `DELETE returns 404 when the service reports not-found`() {
        doThrow(NotFoundException("Exercise", 5L)).whenever(exerciseService).delete(5L, 10L)

        mockMvc
            .perform(delete("/v1/exercises/5"))
            .andExpect(status().isNotFound)
    }
}
