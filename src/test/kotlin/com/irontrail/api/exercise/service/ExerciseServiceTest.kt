package com.irontrail.api.exercise.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.dto.ExercisePatchRequest
import com.irontrail.api.exercise.dto.ExerciseRequest
import com.irontrail.api.exercise.model.Equipment
import com.irontrail.api.exercise.model.Exercise
import com.irontrail.api.exercise.model.ExerciseInputType
import com.irontrail.api.exercise.model.MuscleGroup
import com.irontrail.api.exercise.repository.ExerciseRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class ExerciseServiceTest {

    private val exerciseRepository: ExerciseRepository = mock()
    private val exerciseService = ExerciseService(exerciseRepository)

    private fun exercise(
        id: Long = 5L,
        name: String = "Bench Press",
        ownerId: Long? = 1L,
        primary: MuscleGroup = MuscleGroup.CHEST,
        secondary: List<MuscleGroup> = listOf(MuscleGroup.TRICEPS),
        equipment: Equipment = Equipment.BARBELL,
        inputType: ExerciseInputType = ExerciseInputType.REPS,
        description: String? = "A compound chest exercise",
        wgerId: Int? = null,
        imageUrl: String? = null
    ) = Exercise(
        wgerId = wgerId,
        name = name,
        primaryMuscleGroup = primary,
        secondaryMuscleGroups = secondary,
        equipment = equipment,
        inputType = inputType,
        description = description,
        imageUrl = imageUrl,
        ownerId = ownerId
    ).apply { exerciseId = id }

    private fun exerciseRequest(
        name: String = "Incline Press",
        primary: MuscleGroup = MuscleGroup.CHEST,
        secondary: List<MuscleGroup> = emptyList(),
        equipment: Equipment = Equipment.DUMBBELL,
        inputType: ExerciseInputType = ExerciseInputType.REPS,
        description: String? = "Upper chest focus"
    ) = ExerciseRequest(
        name = name,
        primaryMuscleGroup = primary,
        secondaryMuscleGroups = secondary,
        equipment = equipment,
        inputType = inputType,
        description = description
    )

    // ---- findAll ----

    @Test
    fun `findAll escapes backslash, percent, and underscore in the search term before querying`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll("100%_test\\path", null, 1L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups(
            "100\\%\\_test\\\\path", false, emptyList(), 1L
        )
    }

    @Test
    fun `findAll normalizes a null search to an empty string rather than passing null through`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll(null, null, 77L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups("", false, emptyList(), 77L)
    }

    @Test
    fun `findAll passes a blank search string through unchanged - nothing to escape`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll("", null, 1L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups("", false, emptyList(), 1L)
    }

    @Test
    fun `findAll sets hasMuscleFilter true and forwards the muscle groups unchanged when provided`() {
        val requested = listOf(MuscleGroup.CHEST, MuscleGroup.TRICEPS)
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll("bench", requested, 7L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups("bench", true, requested, 7L)
    }

    @Test
    fun `findAll treats a null muscleGroups list as no filter`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll("bench", null, 1L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups("bench", false, emptyList(), 1L)
    }

    @Test
    fun `findAll treats an explicitly empty muscleGroups list the same as null - no filter applied`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        exerciseService.findAll("bench", emptyList(), 1L)

        verify(exerciseRepository).findVisibleBySearchAndMuscleGroups("bench", false, emptyList(), 1L)
    }

    @Test
    fun `findAll maps every repository result to a response, preserving order`() {
        val global = exercise(id = 1L, name = "Global Squat", ownerId = null)
        val owned = exercise(id = 2L, name = "My Custom Curl", ownerId = 9L)
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(listOf(global, owned))

        val result = exerciseService.findAll(null, null, 9L)

        assertEquals(2, result.size)
        assertEquals("Global Squat", result[0].name)
        assertEquals(1L, result[0].exerciseId)
        assertNull(result[0].ownerId)
        assertEquals("My Custom Curl", result[1].name)
        assertEquals(9L, result[1].ownerId)
    }

    @Test
    fun `findAll returns an empty list, not null, when the repository finds nothing`() {
        whenever(exerciseRepository.findVisibleBySearchAndMuscleGroups(any(), any(), any(), any()))
            .thenReturn(emptyList())

        val result = exerciseService.findAll("nonexistent", null, 1L)

        assertTrue(result.isEmpty())
    }

    // ---- findById ----

    @Test
    fun `findById returns a mapped response for a visible exercise`() {
        val entity = exercise(id = 3L, name = "Deadlift", ownerId = null)
        whenever(exerciseRepository.findVisibleById(3L, 5L)).thenReturn(entity)

        val response = exerciseService.findById(3L, 5L)

        assertEquals(3L, response.exerciseId)
        assertEquals("Deadlift", response.name)
        assertNull(response.ownerId)
    }

    @Test
    fun `findById throws NotFoundException naming the resource and id when the exercise doesn't exist`() {
        whenever(exerciseRepository.findVisibleById(99L, 5L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            exerciseService.findById(99L, 5L)
        }
        assertEquals("Exercise not found: 99", ex.message)
    }

    @Test
    fun `findById masks another user's private exercise as not-found rather than a permission error`() {
        // Visibility is enforced by the repository query; a private exercise belonging to
        // someone else simply never comes back, simulated here as null.
        whenever(exerciseRepository.findVisibleById(4L, 2L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            exerciseService.findById(4L, 2L)
        }
    }

    // ---- create ----

    @Test
    fun `create sets ownerId to the calling user, never null and never another id`() {
        val captor = argumentCaptor<Exercise>()
        whenever(exerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as Exercise }

        exerciseService.create(exerciseRequest(), 42L)

        assertEquals(42L, captor.firstValue.ownerId)
    }

    @Test
    fun `create always sets wgerId and imageUrl to null - not client-suppliable on create`() {
        val captor = argumentCaptor<Exercise>()
        whenever(exerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as Exercise }

        exerciseService.create(exerciseRequest(), 1L)

        assertNull(captor.firstValue.wgerId)
        assertNull(captor.firstValue.imageUrl)
    }

    @Test
    fun `create maps every request field onto the new entity`() {
        val request = ExerciseRequest(
            name = "Incline Press",
            primaryMuscleGroup = MuscleGroup.CHEST,
            secondaryMuscleGroups = listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS),
            equipment = Equipment.DUMBBELL,
            inputType = ExerciseInputType.REPS,
            description = "Upper chest focus"
        )
        val captor = argumentCaptor<Exercise>()
        whenever(exerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as Exercise }

        exerciseService.create(request, 1L)

        val saved = captor.firstValue
        assertEquals("Incline Press", saved.name)
        assertEquals(MuscleGroup.CHEST, saved.primaryMuscleGroup)
        assertEquals(listOf(MuscleGroup.SHOULDERS, MuscleGroup.TRICEPS), saved.secondaryMuscleGroups)
        assertEquals(Equipment.DUMBBELL, saved.equipment)
        assertEquals(ExerciseInputType.REPS, saved.inputType)
        assertEquals("Upper chest focus", saved.description)
    }

    @Test
    fun `create returns the response mapped from the saved (post-insert) entity`() {
        whenever(exerciseRepository.save(any())).thenAnswer {
            (it.arguments[0] as Exercise).apply { exerciseId = 77L }
        }

        val response = exerciseService.create(exerciseRequest(), 1L)

        assertEquals(77L, response.exerciseId)
    }

    @Test
    fun `create defaults secondaryMuscleGroups to empty when the request omits them`() {
        val request = ExerciseRequest(
            name = "Plank",
            primaryMuscleGroup = MuscleGroup.CORE,
            equipment = Equipment.BODYWEIGHT,
            inputType = ExerciseInputType.TIMED,
            description = null
        )
        val captor = argumentCaptor<Exercise>()
        whenever(exerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as Exercise }

        exerciseService.create(request, 1L)

        assertTrue(captor.firstValue.secondaryMuscleGroups.isEmpty())
    }

    @Test
    fun `create allows a null description`() {
        val request = exerciseRequest(description = null)
        val captor = argumentCaptor<Exercise>()
        whenever(exerciseRepository.save(captor.capture())).thenAnswer { it.arguments[0] as Exercise }

        exerciseService.create(request, 1L)

        assertNull(captor.firstValue.description)
    }

    // ---- update (patch) ----

    @Test
    fun `update throws NotFoundException when no exercise is owned by the caller with that id`() {
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            exerciseService.update(5L, ExercisePatchRequest(name = "New Name"), 1L)
        }
        assertEquals("Exercise not found: 5", ex.message)
    }

    @Test
    fun `update masks attempts to edit another user's exercise as not-found, not a permission error`() {
        // findByExerciseIdAndOwnerId is scoped to the caller, so another user's exercise
        // never comes back - simulated here as null, same as a truly missing id.
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 2L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            exerciseService.update(5L, ExercisePatchRequest(name = "Hacked"), 2L)
        }
    }

    @Test
    fun `update masks attempts to edit a global (owner-less) exercise as not-found for any caller`() {
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 3L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            exerciseService.update(5L, ExercisePatchRequest(name = "Rename global"), 3L)
        }
    }

    @Test
    fun `update with every field null leaves the existing exercise completely unchanged`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(), 1L)

        assertEquals("Bench Press", response.name)
        assertEquals(MuscleGroup.CHEST, response.primaryMuscleGroup)
        assertEquals(listOf(MuscleGroup.TRICEPS), response.secondaryMuscleGroups)
        assertEquals(Equipment.BARBELL, response.equipment)
        assertEquals(ExerciseInputType.REPS, response.inputType)
        assertEquals("A compound chest exercise", response.description)
    }

    @Test
    fun `update touching only name leaves every other field unchanged`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(name = "Barbell Bench Press"), 1L)

        assertEquals("Barbell Bench Press", response.name)
        assertEquals(MuscleGroup.CHEST, response.primaryMuscleGroup)
        assertEquals(Equipment.BARBELL, response.equipment)
        assertEquals(ExerciseInputType.REPS, response.inputType)
        assertEquals("A compound chest exercise", response.description)
    }

    @Test
    fun `update touching only primaryMuscleGroup leaves the name and other fields unchanged`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(primaryMuscleGroup = MuscleGroup.BACK), 1L)

        assertEquals(MuscleGroup.BACK, response.primaryMuscleGroup)
        assertEquals("Bench Press", response.name)
        assertEquals(Equipment.BARBELL, response.equipment)
    }

    @Test
    fun `update with an explicitly empty secondaryMuscleGroups list clears existing secondary groups`() {
        val existing = exercise(secondary = listOf(MuscleGroup.TRICEPS, MuscleGroup.SHOULDERS))
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(secondaryMuscleGroups = emptyList()), 1L)

        assertTrue(response.secondaryMuscleGroups.isEmpty())
    }

    @Test
    fun `update cannot clear description back to null - a null patch field means unchanged, not cleared`() {
        val existing = exercise(description = "original description")
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(description = null), 1L)

        assertEquals("original description", response.description)
    }

    @Test
    fun `update with an explicit non-null description overwrites the existing value`() {
        val existing = exercise(description = "original description")
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(description = "new description"), 1L)

        assertEquals("new description", response.description)
    }

    @Test
    fun `update with every field present overwrites every field`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(
            5L,
            ExercisePatchRequest(
                name = "Close-Grip Bench Press",
                primaryMuscleGroup = MuscleGroup.TRICEPS,
                secondaryMuscleGroups = listOf(MuscleGroup.CHEST),
                equipment = Equipment.MACHINE,
                inputType = ExerciseInputType.TIMED,
                description = "Overwritten"
            ),
            1L
        )

        assertEquals("Close-Grip Bench Press", response.name)
        assertEquals(MuscleGroup.TRICEPS, response.primaryMuscleGroup)
        assertEquals(listOf(MuscleGroup.CHEST), response.secondaryMuscleGroups)
        assertEquals(Equipment.MACHINE, response.equipment)
        assertEquals(ExerciseInputType.TIMED, response.inputType)
        assertEquals("Overwritten", response.description)
    }

    @Test
    fun `update never changes the id or ownerId, regardless of which fields are patched`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        val response = exerciseService.update(5L, ExercisePatchRequest(name = "Renamed"), 1L)

        assertEquals(5L, response.exerciseId)
        assertEquals(1L, response.ownerId)
    }

    // ---- delete ----

    @Test
    fun `delete removes the caller's own exercise`() {
        val existing = exercise()
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(existing)

        exerciseService.delete(5L, 1L)

        verify(exerciseRepository).delete(existing)
    }

    @Test
    fun `delete throws NotFoundException and never calls repository delete when the id doesn't exist`() {
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 1L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) {
            exerciseService.delete(5L, 1L)
        }
        assertEquals("Exercise not found: 5", ex.message)
        verify(exerciseRepository, never()).delete(any())
    }

    @Test
    fun `delete masks another user's exercise as not-found and never calls repository delete`() {
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 2L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            exerciseService.delete(5L, 2L)
        }
        verify(exerciseRepository, never()).delete(any())
    }

    @Test
    fun `delete throws NotFoundException rather than a permission error when attempting to delete a global exercise`() {
        whenever(exerciseRepository.findByExerciseIdAndOwnerId(5L, 3L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            exerciseService.delete(5L, 3L)
        }
        verify(exerciseRepository, never()).delete(any())
    }
}
