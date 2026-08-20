package com.irontrail.api.split.service

import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.model.SetType
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.TemplateSet
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.TemplateExerciseRepository
import com.irontrail.api.split.repository.TemplateSetRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

class SplitOwnershipResolverTest {
    private val splitRepository: SplitRepository = mock()
    private val workoutDayRepository: WorkoutDayRepository = mock()
    private val templateExerciseRepository: TemplateExerciseRepository = mock()
    private val templateSetRepository: TemplateSetRepository = mock()

    private val resolver =
        SplitOwnershipResolver(
            splitRepository,
            workoutDayRepository,
            templateExerciseRepository,
            templateSetRepository,
        )

    // ---- getOwnedSplit ----

    @Test
    fun `getOwnedSplit returns the split when the repository finds it owned by the caller`() {
        val split = Split(ownerId = 10L, name = "PPL").apply { splitId = 1L }
        whenever(splitRepository.findBySplitIdAndOwnerId(1L, 10L)).thenReturn(split)

        val result = resolver.getOwnedSplit(1L, 10L)

        assertEquals(split, result)
    }

    @Test
    fun `getOwnedSplit throws NotFoundException when no split exists with that id`() {
        whenever(splitRepository.findBySplitIdAndOwnerId(999L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) { resolver.getOwnedSplit(999L, 10L) }

        assertEquals("Split not found: 999", ex.message)
    }

    @Test
    fun `getOwnedSplit throws the same NotFoundException when the split exists but is owned by someone else`() {
        // The repository query filters by owner in the WHERE clause, so a split owned by another
        // user comes back null too - indistinguishable from "doesn't exist" at this layer, by design
        // (enumeration-avoidance, same as the rest of the app's 404-not-403 convention).
        whenever(splitRepository.findBySplitIdAndOwnerId(1L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) { resolver.getOwnedSplit(1L, 10L) }

        assertEquals("Split not found: 1", ex.message)
    }

    // ---- getOwnedWorkoutDay ----

    @Test
    fun `getOwnedWorkoutDay returns the day when the repository finds it owned by the caller`() {
        val day = WorkoutDay(splitId = 1L, name = "Day A", sortOrder = 0).apply { workoutDayId = 100L }
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(100L, 10L)).thenReturn(day)

        val result = resolver.getOwnedWorkoutDay(100L, 10L)

        assertEquals(day, result)
    }

    @Test
    fun `getOwnedWorkoutDay throws NotFoundException when no day exists with that id`() {
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(999L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) { resolver.getOwnedWorkoutDay(999L, 10L) }

        assertEquals("WorkoutDay not found: 999", ex.message)
    }

    @Test
    fun `getOwnedWorkoutDay throws the same NotFoundException when the day exists but is owned by someone else`() {
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(100L, 10L)).thenReturn(null)

        val ex = assertThrows(NotFoundException::class.java) { resolver.getOwnedWorkoutDay(100L, 10L) }

        assertEquals("WorkoutDay not found: 100", ex.message)
    }

    @Test
    fun `getOwnedWorkoutDay issues exactly one repository call, no cascading lookup through Split`() {
        val day = WorkoutDay(splitId = 1L, name = "Day A", sortOrder = 0).apply { workoutDayId = 100L }
        whenever(workoutDayRepository.findOwnedByWorkoutDayId(100L, 10L)).thenReturn(day)

        resolver.getOwnedWorkoutDay(100L, 10L)

        verify(workoutDayRepository).findOwnedByWorkoutDayId(100L, 10L)
        verifyNoInteractions(splitRepository)
    }

    // ---- getOwnedTemplateExercise ----

    @Test
    fun `getOwnedTemplateExercise returns the exercise when the repository finds it owned by the caller`() {
        val te = TemplateExercise(workoutDayId = 100L, exerciseId = 5L, sortOrder = 0).apply { templateExerciseId = 200L }
        whenever(templateExerciseRepository.findOwnedByTemplateExerciseId(200L, 10L)).thenReturn(te)

        val result = resolver.getOwnedTemplateExercise(200L, 10L)

        assertEquals(te, result)
    }

    @Test
    fun `getOwnedTemplateExercise throws NotFoundException when no template exercise exists with that id`() {
        whenever(templateExerciseRepository.findOwnedByTemplateExerciseId(999L, 10L)).thenReturn(null)

        val ex =
            assertThrows(NotFoundException::class.java) {
                resolver.getOwnedTemplateExercise(999L, 10L)
            }

        assertEquals("TemplateExercise not found: 999", ex.message)
    }

    @Test
    fun `getOwnedTemplateExercise throws the same NotFoundException when owned by someone else`() {
        whenever(templateExerciseRepository.findOwnedByTemplateExerciseId(200L, 10L)).thenReturn(null)

        val ex =
            assertThrows(NotFoundException::class.java) {
                resolver.getOwnedTemplateExercise(200L, 10L)
            }

        assertEquals("TemplateExercise not found: 200", ex.message)
    }

    // ---- getOwnedTemplateSet ----

    @Test
    fun `getOwnedTemplateSet returns the set when the repository finds it owned by the caller`() {
        val ts = TemplateSet(sortOrder = 0, setType = SetType.NORMAL).apply { templateSetId = 300L }
        whenever(templateSetRepository.findOwnedByTemplateSetId(300L, 10L)).thenReturn(ts)

        val result = resolver.getOwnedTemplateSet(300L, 10L)

        assertEquals(ts, result)
    }

    @Test
    fun `getOwnedTemplateSet throws NotFoundException when no template set exists with that id`() {
        whenever(templateSetRepository.findOwnedByTemplateSetId(999L, 10L)).thenReturn(null)

        val ex =
            assertThrows(NotFoundException::class.java) {
                resolver.getOwnedTemplateSet(999L, 10L)
            }

        assertEquals("TemplateSet not found: 999", ex.message)
    }

    @Test
    fun `getOwnedTemplateSet throws the same NotFoundException when owned by someone else`() {
        whenever(templateSetRepository.findOwnedByTemplateSetId(300L, 10L)).thenReturn(null)

        val ex =
            assertThrows(NotFoundException::class.java) {
                resolver.getOwnedTemplateSet(300L, 10L)
            }

        assertEquals("TemplateSet not found: 300", ex.message)
    }

    @Test
    fun `getOwnedTemplateSet issues exactly one repository call, no cascading lookups through parent levels`() {
        val ts = TemplateSet(sortOrder = 0, setType = SetType.NORMAL).apply { templateSetId = 300L }
        whenever(templateSetRepository.findOwnedByTemplateSetId(300L, 10L)).thenReturn(ts)

        resolver.getOwnedTemplateSet(300L, 10L)

        verify(templateSetRepository).findOwnedByTemplateSetId(300L, 10L)
        verifyNoInteractions(splitRepository, workoutDayRepository, templateExerciseRepository)
    }
}
