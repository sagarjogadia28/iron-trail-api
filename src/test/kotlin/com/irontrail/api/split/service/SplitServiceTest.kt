package com.irontrail.api.split.service

import com.irontrail.api.common.BadRequestException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.exercise.repository.ExerciseRepository
import com.irontrail.api.split.dto.SplitPatchRequest
import com.irontrail.api.split.dto.SplitRequest
import com.irontrail.api.split.dto.TemplateExercisePatchRequest
import com.irontrail.api.split.dto.TemplateExerciseRequest
import com.irontrail.api.split.dto.TemplateSetPatchRequest
import com.irontrail.api.split.dto.TemplateSetRequest
import com.irontrail.api.split.dto.WorkoutDayPatchRequest
import com.irontrail.api.split.dto.WorkoutDayRequest
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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SplitServiceTest {
    private val splitRepository: SplitRepository = mock()
    private val workoutDayRepository: WorkoutDayRepository = mock()
    private val templateExerciseRepository: TemplateExerciseRepository = mock()
    private val templateSetRepository: TemplateSetRepository = mock()
    private val exerciseRepository: ExerciseRepository = mock()
    private val ownershipResolver: SplitOwnershipResolver = mock()

    private val service =
        SplitService(
            splitRepository,
            workoutDayRepository,
            templateExerciseRepository,
            templateSetRepository,
            exerciseRepository,
            ownershipResolver,
        )

    // ---- fixtures ----

    private fun split(
        id: Long = 1L,
        ownerId: Long = 10L,
        name: String = "Push Pull Legs",
    ) = Split(ownerId = ownerId, name = name).apply { splitId = id }

    private fun workoutDay(
        id: Long = 1L,
        splitId: Long = 1L,
        name: String = "Push Day",
        sortOrder: Int = 0,
    ) = WorkoutDay(splitId = splitId, name = name, sortOrder = sortOrder).apply { workoutDayId = id }

    private fun templateExercise(
        id: Long = 1L,
        workoutDayId: Long = 1L,
        exerciseId: Long = 100L,
        sortOrder: Int = 0,
        restDurationSeconds: Int = 90,
        isRepRange: Boolean = true,
        notes: String? = null,
    ) = TemplateExercise(
        workoutDayId = workoutDayId,
        exerciseId = exerciseId,
        sortOrder = sortOrder,
        restDurationSeconds = restDurationSeconds,
        isRepRange = isRepRange,
        notes = notes,
    ).apply { templateExerciseId = id }

    private fun templateSet(
        id: Long = 1L,
        parent: TemplateExercise,
        sortOrder: Int = 0,
        targetReps: Int? = 8,
        targetRepsMax: Int? = null,
        targetDurationSeconds: Int? = null,
        setType: SetType = SetType.NORMAL,
    ) = TemplateSet(
        sortOrder = sortOrder,
        targetReps = targetReps,
        targetRepsMax = targetRepsMax,
        targetDurationSeconds = targetDurationSeconds,
        setType = setType,
    ).apply {
        templateSetId = id
        templateExercise = parent
    }

    private fun stubEmptyTree() {
        whenever(workoutDayRepository.findBySplitIdIn(any())).thenReturn(emptyList())
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(any())).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(any())).thenReturn(emptyList())
    }

    // ---- findAll ----

    @Test
    fun `findAll returns an empty list when the caller owns no splits`() {
        whenever(splitRepository.findByOwnerId(10L)).thenReturn(emptyList())

        val result = service.findAll(10L)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findAll never queries another user's splits - delegates filtering to the repository`() {
        whenever(splitRepository.findByOwnerId(10L)).thenReturn(emptyList())

        service.findAll(10L)

        verify(splitRepository).findByOwnerId(10L)
    }

    @Test
    fun `findAll computes workoutDayCount and exerciseCount from the assembled tree, not stored columns`() {
        val s = split(id = 1L)
        val day1 = workoutDay(id = 10L, splitId = 1L)
        val day2 = workoutDay(id = 11L, splitId = 1L, sortOrder = 1)
        val ex1 = templateExercise(id = 100L, workoutDayId = 10L)
        val ex2 = templateExercise(id = 101L, workoutDayId = 10L, sortOrder = 1)
        val ex3 = templateExercise(id = 102L, workoutDayId = 11L)
        whenever(splitRepository.findByOwnerId(10L)).thenReturn(listOf(s))
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(day1, day2))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L, 11L))).thenReturn(listOf(ex1, ex2, ex3))
        whenever(templateSetRepository.findByTemplateExerciseIn(listOf(ex1, ex2, ex3))).thenReturn(emptyList())

        val result = service.findAll(10L)

        assertEquals(1, result.size)
        assertEquals(2, result[0].workoutDayCount)
        assertEquals(3, result[0].exerciseCount)
    }

    @Test
    fun `findAll keeps each split's workout days isolated from another split's days`() {
        val splitA = split(id = 1L, name = "A")
        val splitB = split(id = 2L, name = "B")
        val dayInA = workoutDay(id = 10L, splitId = 1L)
        val dayInB = workoutDay(id = 20L, splitId = 2L)
        whenever(splitRepository.findByOwnerId(10L)).thenReturn(listOf(splitA, splitB))
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L, 2L))).thenReturn(listOf(dayInA, dayInB))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L, 20L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        val result = service.findAll(10L)

        assertEquals(1, result.first { it.splitId == 1L }.workoutDayCount)
        assertEquals(1, result.first { it.splitId == 2L }.workoutDayCount)
    }

    // ---- findById ----

    @Test
    fun `findById returns the nested tree with workout days sorted by sortOrder`() {
        val s = split(id = 1L)
        val dayLater = workoutDay(id = 11L, splitId = 1L, name = "Legs", sortOrder = 1)
        val dayEarlier = workoutDay(id = 10L, splitId = 1L, name = "Push", sortOrder = 0)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(dayLater, dayEarlier))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(11L, 10L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        val result = service.findById(1L, 10L)

        assertEquals(listOf("Push", "Legs"), result.workoutDays.map { it.name })
    }

    @Test
    fun `findById sorts template exercises within a day, and sets within an exercise, by sortOrder`() {
        val s = split(id = 1L)
        val day = workoutDay(id = 10L, splitId = 1L)
        val exLater = templateExercise(id = 101L, workoutDayId = 10L, sortOrder = 1)
        val exEarlier = templateExercise(id = 100L, workoutDayId = 10L, sortOrder = 0)
        val setLater = templateSet(id = 1001L, parent = exEarlier, sortOrder = 1)
        val setEarlier = templateSet(id = 1000L, parent = exEarlier, sortOrder = 0)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(day))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L))).thenReturn(listOf(exLater, exEarlier))
        whenever(templateSetRepository.findByTemplateExerciseIn(listOf(exLater, exEarlier)))
            .thenReturn(listOf(setLater, setEarlier))

        val result = service.findById(1L, 10L)

        assertEquals(listOf(100L, 101L), result.workoutDays[0].templateExercises.map { it.templateExerciseId })
        assertEquals(
            listOf(1000L, 1001L),
            result.workoutDays[0]
                .templateExercises[0]
                .templateSets
                .map { it.templateSetId },
        )
    }

    @Test
    fun `findById propagates NotFoundException when the split isn't owned by the caller`() {
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenThrow(NotFoundException("Split", 1L))

        assertThrows(NotFoundException::class.java) { service.findById(1L, 10L) }
    }

    // ---- create ----

    @Test
    fun `create saves a split owned by the caller with the requested name`() {
        val captor = argumentCaptor<Split>()
        whenever(splitRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 5L } }

        service.create(SplitRequest(name = "New Split"), 10L)

        assertEquals(10L, captor.firstValue.ownerId)
        assertEquals("New Split", captor.firstValue.name)
    }

    @Test
    fun `create returns an empty workoutDays tree without querying for children - none can exist yet`() {
        whenever(splitRepository.save(any())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 5L } }

        val result = service.create(SplitRequest(name = "New Split"), 10L)

        assertTrue(result.workoutDays.isEmpty())
        verify(workoutDayRepository, never()).findBySplitIdIn(any())
    }

    // ---- update ----

    @Test
    fun `update overwrites the name when supplied`() {
        val s = split(name = "Old Name")
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)
        stubEmptyTree()

        val result = service.update(1L, SplitPatchRequest(name = "New Name"), 10L)

        assertEquals("New Name", result.name)
    }

    @Test
    fun `update with a null name leaves the split's name unchanged`() {
        val s = split(name = "Original")
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)
        stubEmptyTree()

        val result = service.update(1L, SplitPatchRequest(name = null), 10L)

        assertEquals("Original", result.name)
    }

    @Test
    fun `update returns the split's current children tree, not an empty one`() {
        val s = split(id = 1L)
        val day = workoutDay(id = 10L, splitId = 1L)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(day))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        val result = service.update(1L, SplitPatchRequest(name = "Renamed"), 10L)

        assertEquals(1, result.workoutDays.size)
    }

    @Test
    fun `update propagates NotFoundException when the split isn't owned by the caller and never saves`() {
        whenever(ownershipResolver.getOwnedSplit(1L, 2L)).thenThrow(NotFoundException("Split", 1L))

        assertThrows(NotFoundException::class.java) { service.update(1L, SplitPatchRequest(name = "Hacked"), 2L) }
        verify(splitRepository, never()).save(any())
    }

    // ---- delete ----

    @Test
    fun `delete removes the caller's own split`() {
        val s = split()
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(s)

        service.delete(1L, 10L)

        verify(splitRepository).delete(s)
    }

    @Test
    fun `delete propagates NotFoundException and never calls repository delete`() {
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenThrow(NotFoundException("Split", 1L))

        assertThrows(NotFoundException::class.java) { service.delete(1L, 10L) }
        verify(splitRepository, never()).delete(any())
    }

    // ---- duplicateSplit ----

    @Test
    fun `duplicateSplit creates a new split owned by the caller using the request's name, not the source split's name`() {
        val source = split(id = 1L, ownerId = 10L, name = "Original")
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(source)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(any())).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(any())).thenReturn(emptyList())
        val captor = argumentCaptor<Split>()
        whenever(splitRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 99L } }
        whenever(workoutDayRepository.findBySplitIdIn(listOf(99L))).thenReturn(emptyList())

        val result = service.duplicateSplit(1L, SplitRequest(name = "Original (Copy)"), 10L)

        assertEquals(10L, captor.firstValue.ownerId)
        assertEquals("Original (Copy)", captor.firstValue.name)
        assertEquals(99L, result.splitId)
    }

    @Test
    fun `duplicateSplit deep-copies each source day's name and sortOrder onto a new day in the new split`() {
        val source = split(id = 1L, ownerId = 10L)
        val sourceDay = workoutDay(id = 10L, splitId = 1L, name = "Push Day", sortOrder = 2)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(source)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(sourceDay))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        whenever(splitRepository.save(any())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 99L } }
        val daysCaptor = argumentCaptor<List<WorkoutDay>>()
        whenever(workoutDayRepository.saveAll(daysCaptor.capture())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<WorkoutDay>).mapIndexed { idx, d -> d.apply { workoutDayId = 200L + idx } }
        }
        whenever(templateExerciseRepository.saveAll(any<List<TemplateExercise>>())).thenReturn(emptyList())
        whenever(templateSetRepository.saveAll(any<List<TemplateSet>>())).thenReturn(emptyList())
        whenever(workoutDayRepository.findBySplitIdIn(listOf(99L))).thenReturn(emptyList())

        service.duplicateSplit(1L, SplitRequest(name = "Copy"), 10L)

        val savedDay = daysCaptor.firstValue.single()
        assertEquals("Push Day", savedDay.name)
        assertEquals(2, savedDay.sortOrder)
        assertEquals(99L, savedDay.splitId)
    }

    @Test
    fun `duplicateSplit remaps template exercises and sets onto the new day ids, not the source day ids`() {
        val source = split(id = 1L, ownerId = 10L)
        val day1 = workoutDay(id = 10L, splitId = 1L)
        val day2 = workoutDay(id = 11L, splitId = 1L, sortOrder = 1)
        val ex1 = templateExercise(id = 100L, workoutDayId = 10L, exerciseId = 500L)
        val ex2 = templateExercise(id = 101L, workoutDayId = 11L, exerciseId = 501L)
        val set1 = templateSet(id = 1000L, parent = ex1, targetReps = 8)
        val set2 = templateSet(id = 1001L, parent = ex2, targetReps = 12)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(source)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(day1, day2))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L, 11L))).thenReturn(listOf(ex1, ex2))
        whenever(templateSetRepository.findByTemplateExerciseIn(listOf(ex1, ex2))).thenReturn(listOf(set1, set2))
        whenever(splitRepository.save(any())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 99L } }
        whenever(workoutDayRepository.saveAll(any<List<WorkoutDay>>())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<WorkoutDay>).mapIndexed { idx, d -> d.apply { workoutDayId = 900L + idx } }
        }
        val exercisesCaptor = argumentCaptor<List<TemplateExercise>>()
        whenever(templateExerciseRepository.saveAll(exercisesCaptor.capture())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<TemplateExercise>).mapIndexed { idx, e -> e.apply { templateExerciseId = 800L + idx } }
        }
        val setsCaptor = argumentCaptor<List<TemplateSet>>()
        whenever(templateSetRepository.saveAll(setsCaptor.capture())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<TemplateSet>).mapIndexed { idx, s -> s.apply { templateSetId = 700L + idx } }
        }
        whenever(workoutDayRepository.findBySplitIdIn(listOf(99L))).thenReturn(emptyList())

        service.duplicateSplit(1L, SplitRequest(name = "Copy"), 10L)

        val newExercises = exercisesCaptor.firstValue
        assertEquals(setOf(900L, 901L), newExercises.map { it.workoutDayId }.toSet())
        assertEquals(setOf(500L, 501L), newExercises.map { it.exerciseId }.toSet())

        val newSets = setsCaptor.firstValue
        assertEquals(setOf(8, 12), newSets.map { it.targetReps }.toSet())
        // Every new set's parent must be one of the newly-saved exercises, never a source exercise.
        newSets.forEach { assertTrue(it.templateExercise in newExercises) }
    }

    @Test
    fun `duplicateSplit on a split with no workout days creates an empty copy without error`() {
        val source = split(id = 1L, ownerId = 10L)
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(source)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(emptyList())).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        whenever(splitRepository.save(any())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 99L } }
        whenever(workoutDayRepository.saveAll(any<List<WorkoutDay>>())).thenReturn(emptyList())
        whenever(templateExerciseRepository.saveAll(any<List<TemplateExercise>>())).thenReturn(emptyList())
        whenever(templateSetRepository.saveAll(any<List<TemplateSet>>())).thenReturn(emptyList())
        whenever(workoutDayRepository.findBySplitIdIn(listOf(99L))).thenReturn(emptyList())

        val result = service.duplicateSplit(1L, SplitRequest(name = "Copy"), 10L)

        assertTrue(result.workoutDays.isEmpty())
    }

    @Test
    fun `duplicateSplit never deletes or mutates the source split's rows`() {
        val source = split(id = 1L, ownerId = 10L)
        val sourceDay = workoutDay(id = 10L, splitId = 1L, name = "Push Day")
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(source)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(1L))).thenReturn(listOf(sourceDay))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(10L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        whenever(splitRepository.save(any())).thenAnswer { (it.arguments[0] as Split).apply { splitId = 99L } }
        whenever(workoutDayRepository.saveAll(any<List<WorkoutDay>>())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<WorkoutDay>).mapIndexed { idx, d -> d.apply { workoutDayId = 200L + idx } }
        }
        whenever(templateExerciseRepository.saveAll(any<List<TemplateExercise>>())).thenReturn(emptyList())
        whenever(templateSetRepository.saveAll(any<List<TemplateSet>>())).thenReturn(emptyList())
        whenever(workoutDayRepository.findBySplitIdIn(listOf(99L))).thenReturn(emptyList())

        service.duplicateSplit(1L, SplitRequest(name = "Copy"), 10L)

        verify(splitRepository, never()).delete(any())
        verify(workoutDayRepository, never()).delete(any())
        assertEquals("Push Day", sourceDay.name) // untouched
    }

    // ---- createWorkoutDay ----

    @Test
    fun `createWorkoutDay checks ownership of the parent split before saving`() {
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenThrow(NotFoundException("Split", 1L))

        assertThrows(NotFoundException::class.java) {
            service.createWorkoutDay(1L, WorkoutDayRequest(name = "Push", sortOrder = 0), 10L)
        }
        verify(workoutDayRepository, never()).save(any())
    }

    @Test
    fun `createWorkoutDay saves under the given split with the requested name and sortOrder`() {
        whenever(ownershipResolver.getOwnedSplit(1L, 10L)).thenReturn(split(id = 1L))
        val captor = argumentCaptor<WorkoutDay>()
        whenever(workoutDayRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as WorkoutDay).apply { workoutDayId = 10L } }

        val result = service.createWorkoutDay(1L, WorkoutDayRequest(name = "Push", sortOrder = 3), 10L)

        assertEquals(1L, captor.firstValue.splitId)
        assertEquals("Push", captor.firstValue.name)
        assertEquals(3, captor.firstValue.sortOrder)
        assertTrue(result.templateExercises.isEmpty())
    }

    // ---- updateWorkoutDay ----

    @Test
    fun `updateWorkoutDay patches name and sortOrder independently when supplied`() {
        val day = workoutDay(name = "Old", sortOrder = 0)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(day)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        val result = service.updateWorkoutDay(1L, WorkoutDayPatchRequest(name = "New", sortOrder = null), 10L)

        assertEquals("New", result.name)
        assertEquals(0, result.sortOrder)
    }

    @Test
    fun `updateWorkoutDay with every field null leaves the day unchanged`() {
        val day = workoutDay(name = "Push Day", sortOrder = 2)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(day)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        val result = service.updateWorkoutDay(1L, WorkoutDayPatchRequest(), 10L)

        assertEquals("Push Day", result.name)
        assertEquals(2, result.sortOrder)
    }

    @Test
    fun `updateWorkoutDay returns the day's current template exercises, freshly queried`() {
        val day = workoutDay(id = 1L)
        val ex = templateExercise(id = 100L, workoutDayId = 1L)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(day)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(listOf(ex))
        whenever(templateSetRepository.findByTemplateExerciseIn(listOf(ex))).thenReturn(emptyList())

        val result = service.updateWorkoutDay(1L, WorkoutDayPatchRequest(name = "Renamed"), 10L)

        assertEquals(1, result.templateExercises.size)
    }

    @Test
    fun `updateWorkoutDay propagates NotFoundException when not owned by the caller`() {
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 2L)).thenThrow(NotFoundException("WorkoutDay", 1L))

        assertThrows(NotFoundException::class.java) {
            service.updateWorkoutDay(1L, WorkoutDayPatchRequest(name = "Hacked"), 2L)
        }
    }

    // ---- deleteWorkoutDay ----

    @Test
    fun `deleteWorkoutDay removes the caller's own day`() {
        val day = workoutDay()
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(day)

        service.deleteWorkoutDay(1L, 10L)

        verify(workoutDayRepository).delete(day)
    }

    @Test
    fun `deleteWorkoutDay propagates NotFoundException and never calls repository delete`() {
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenThrow(NotFoundException("WorkoutDay", 1L))

        assertThrows(NotFoundException::class.java) { service.deleteWorkoutDay(1L, 10L) }
        verify(workoutDayRepository, never()).delete(any())
    }

    // ---- duplicateWorkoutDay ----

    @Test
    fun `duplicateWorkoutDay creates the copy in the same split as the source day`() {
        val sourceDay = workoutDay(id = 1L, splitId = 7L, name = "Push Day")
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(sourceDay)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        val captor = argumentCaptor<WorkoutDay>()
        whenever(workoutDayRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as WorkoutDay).apply { workoutDayId = 20L } }
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(20L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())

        service.duplicateWorkoutDay(1L, WorkoutDayRequest(name = "Push Day (Copy)", sortOrder = 1), 10L)

        assertEquals(7L, captor.firstValue.splitId)
    }

    @Test
    fun `duplicateWorkoutDay uses the request's name and sortOrder, not the source day's`() {
        val sourceDay = workoutDay(id = 1L, splitId = 7L, name = "Push Day", sortOrder = 0)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(sourceDay)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        val captor = argumentCaptor<WorkoutDay>()
        whenever(workoutDayRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as WorkoutDay).apply { workoutDayId = 20L } }
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(20L))).thenReturn(emptyList())

        service.duplicateWorkoutDay(1L, WorkoutDayRequest(name = "Push Day (Copy)", sortOrder = 5), 10L)

        assertEquals("Push Day (Copy)", captor.firstValue.name)
        assertEquals(5, captor.firstValue.sortOrder)
    }

    @Test
    fun `duplicateWorkoutDay copies the source day's exercises and sets onto the new day`() {
        val sourceDay = workoutDay(id = 1L, splitId = 7L)
        val ex = templateExercise(id = 100L, workoutDayId = 1L, exerciseId = 500L, notes = "keep form strict")
        val set = templateSet(id = 1000L, parent = ex, targetReps = 10)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(sourceDay)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(listOf(ex))
        whenever(templateSetRepository.findByTemplateExerciseIn(listOf(ex))).thenReturn(listOf(set))
        whenever(workoutDayRepository.save(any())).thenAnswer { (it.arguments[0] as WorkoutDay).apply { workoutDayId = 20L } }
        val exercisesCaptor = argumentCaptor<List<TemplateExercise>>()
        whenever(templateExerciseRepository.saveAll(exercisesCaptor.capture())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<TemplateExercise>).mapIndexed { idx, e -> e.apply { templateExerciseId = 800L + idx } }
        }
        val setsCaptor = argumentCaptor<List<TemplateSet>>()
        whenever(templateSetRepository.saveAll(setsCaptor.capture())).thenAnswer {
            @Suppress("UNCHECKED_CAST")
            (it.arguments[0] as List<TemplateSet>).mapIndexed { idx, s -> s.apply { templateSetId = 700L + idx } }
        }
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(20L))).thenReturn(emptyList())

        service.duplicateWorkoutDay(1L, WorkoutDayRequest(name = "Copy", sortOrder = 0), 10L)

        val newExercise = exercisesCaptor.firstValue.single()
        assertEquals(20L, newExercise.workoutDayId)
        assertEquals(500L, newExercise.exerciseId)
        assertEquals("keep form strict", newExercise.notes)
        val newSet = setsCaptor.firstValue.single()
        assertEquals(10, newSet.targetReps)
        assertEquals(newExercise, newSet.templateExercise)
    }

    @Test
    fun `duplicateWorkoutDay on a day with no exercises creates an empty copy without error`() {
        val sourceDay = workoutDay(id = 1L, splitId = 7L)
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(sourceDay)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        whenever(templateSetRepository.findByTemplateExerciseIn(emptyList())).thenReturn(emptyList())
        whenever(workoutDayRepository.save(any())).thenAnswer { (it.arguments[0] as WorkoutDay).apply { workoutDayId = 20L } }
        whenever(templateExerciseRepository.saveAll(any<List<TemplateExercise>>())).thenReturn(emptyList())
        whenever(templateSetRepository.saveAll(any<List<TemplateSet>>())).thenReturn(emptyList())
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(20L))).thenReturn(emptyList())

        val result = service.duplicateWorkoutDay(1L, WorkoutDayRequest(name = "Copy", sortOrder = 0), 10L)

        assertTrue(result.templateExercises.isEmpty())
    }

    // ---- createTemplateExercise ----

    @Test
    fun `createTemplateExercise checks ownership of the parent day before checking exercise visibility`() {
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenThrow(NotFoundException("WorkoutDay", 1L))

        assertThrows(NotFoundException::class.java) {
            service.createTemplateExercise(1L, TemplateExerciseRequest(exerciseId = 500L, sortOrder = 0), 10L)
        }
        verify(exerciseRepository, never()).existsVisibleById(any(), any())
    }

    @Test
    fun `createTemplateExercise throws NotFoundException naming Exercise when the exercise isn't visible to the caller`() {
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(workoutDay(id = 1L))
        whenever(exerciseRepository.existsVisibleById(500L, 10L)).thenReturn(false)

        val ex =
            assertThrows(NotFoundException::class.java) {
                service.createTemplateExercise(1L, TemplateExerciseRequest(exerciseId = 500L, sortOrder = 0), 10L)
            }
        assertEquals("Exercise not found: 500", ex.message)
        verify(templateExerciseRepository, never()).save(any())
    }

    @Test
    fun `createTemplateExercise saves every request field under the given workout day`() {
        whenever(ownershipResolver.getOwnedWorkoutDay(1L, 10L)).thenReturn(workoutDay(id = 1L))
        whenever(exerciseRepository.existsVisibleById(500L, 10L)).thenReturn(true)
        val captor = argumentCaptor<TemplateExercise>()
        whenever(templateExerciseRepository.save(captor.capture()))
            .thenAnswer { (it.arguments[0] as TemplateExercise).apply { templateExerciseId = 100L } }

        val result =
            service.createTemplateExercise(
                1L,
                TemplateExerciseRequest(
                    exerciseId = 500L,
                    sortOrder = 2,
                    restDurationSeconds = 60,
                    isRepRange = false,
                    notes = "slow eccentric",
                ),
                10L,
            )

        assertEquals(1L, captor.firstValue.workoutDayId)
        assertEquals(500L, captor.firstValue.exerciseId)
        assertEquals(2, captor.firstValue.sortOrder)
        assertEquals(60, captor.firstValue.restDurationSeconds)
        assertEquals(false, captor.firstValue.isRepRange)
        assertEquals("slow eccentric", captor.firstValue.notes)
        assertTrue(result.templateSets.isEmpty())
    }

    // ---- updateTemplateExercise ----

    @Test
    fun `updateTemplateExercise patches only the fields supplied, leaving others untouched`() {
        val te = templateExercise(sortOrder = 0, restDurationSeconds = 90, isRepRange = true, notes = "original")
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)

        val result = service.updateTemplateExercise(1L, TemplateExercisePatchRequest(restDurationSeconds = 120), 10L)

        assertEquals(120, result.restDurationSeconds)
        assertEquals(0, result.sortOrder)
        assertEquals(true, result.isRepRange)
        assertEquals("original", result.notes)
    }

    @Test
    fun `updateTemplateExercise cannot change exerciseId - the patch DTO has no such field`() {
        val te = templateExercise(exerciseId = 500L)
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)

        val result = service.updateTemplateExercise(1L, TemplateExercisePatchRequest(notes = "updated"), 10L)

        assertEquals(500L, result.exerciseId)
    }

    @Test
    fun `updateTemplateExercise reads sets from the entity's own relation, never re-queries the repository`() {
        val te = templateExercise(id = 1L)
        val set = templateSet(id = 10L, parent = te, sortOrder = 0)
        te.sets.add(set)
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)

        val result = service.updateTemplateExercise(1L, TemplateExercisePatchRequest(notes = "updated"), 10L)

        assertEquals(1, result.templateSets.size)
        assertEquals(10L, result.templateSets[0].templateSetId)
        verify(templateSetRepository, never()).findByTemplateExerciseIn(any())
    }

    @Test
    fun `updateTemplateExercise sorts sets by sortOrder from the entity relation`() {
        val te = templateExercise(id = 1L)
        val setLater = templateSet(id = 11L, parent = te, sortOrder = 1)
        val setEarlier = templateSet(id = 10L, parent = te, sortOrder = 0)
        te.sets.add(setLater)
        te.sets.add(setEarlier)
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)

        val result = service.updateTemplateExercise(1L, TemplateExercisePatchRequest(), 10L)

        assertEquals(listOf(10L, 11L), result.templateSets.map { it.templateSetId })
    }

    // ---- deleteTemplateExercise ----

    @Test
    fun `deleteTemplateExercise removes the caller's own template exercise`() {
        val te = templateExercise()
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)

        service.deleteTemplateExercise(1L, 10L)

        verify(templateExerciseRepository).delete(te)
    }

    @Test
    fun `deleteTemplateExercise propagates NotFoundException and never calls repository delete`() {
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenThrow(NotFoundException("TemplateExercise", 1L))

        assertThrows(NotFoundException::class.java) { service.deleteTemplateExercise(1L, 10L) }
        verify(templateExerciseRepository, never()).delete(any())
    }

    // ---- createTemplateSet ----

    @Test
    fun `createTemplateSet attaches the saved set to the owned parent exercise`() {
        val te = templateExercise(id = 1L)
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)
        val captor = argumentCaptor<TemplateSet>()
        whenever(templateSetRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as TemplateSet).apply { templateSetId = 10L } }

        service.createTemplateSet(1L, TemplateSetRequest(sortOrder = 0, targetReps = 8, setType = SetType.NORMAL), 10L)

        assertEquals(te, captor.firstValue.templateExercise)
    }

    @Test
    fun `createTemplateSet saves every request field`() {
        val te = templateExercise(id = 1L)
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(te)
        val captor = argumentCaptor<TemplateSet>()
        whenever(templateSetRepository.save(captor.capture())).thenAnswer { (it.arguments[0] as TemplateSet).apply { templateSetId = 10L } }

        service.createTemplateSet(
            1L,
            TemplateSetRequest(sortOrder = 3, targetReps = 6, targetRepsMax = 10, targetDurationSeconds = null, setType = SetType.DROP_SET),
            10L,
        )

        assertEquals(3, captor.firstValue.sortOrder)
        assertEquals(6, captor.firstValue.targetReps)
        assertEquals(10, captor.firstValue.targetRepsMax)
        assertEquals(SetType.DROP_SET, captor.firstValue.setType)
    }

    @Test
    fun `createTemplateSet allows targetReps equal to targetRepsMax - a fixed rep count, not a range`() {
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(templateExercise(id = 1L))
        whenever(templateSetRepository.save(any())).thenAnswer { (it.arguments[0] as TemplateSet).apply { templateSetId = 10L } }

        service.createTemplateSet(
            1L,
            TemplateSetRequest(sortOrder = 0, targetReps = 8, targetRepsMax = 8, setType = SetType.NORMAL),
            10L,
        )

        verify(templateSetRepository).save(any())
    }

    @Test
    fun `createTemplateSet rejects targetReps greater than targetRepsMax and never saves`() {
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(templateExercise(id = 1L))

        val ex =
            assertThrows(BadRequestException::class.java) {
                service.createTemplateSet(
                    1L,
                    TemplateSetRequest(sortOrder = 0, targetReps = 12, targetRepsMax = 8, setType = SetType.NORMAL),
                    10L,
                )
            }
        assertEquals("targetReps must not exceed targetRepsMax", ex.message)
        verify(templateSetRepository, never()).save(any())
    }

    @Test
    fun `createTemplateSet allows a null targetRepsMax - not a range, no comparison to make`() {
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenReturn(templateExercise(id = 1L))
        whenever(templateSetRepository.save(any())).thenAnswer { (it.arguments[0] as TemplateSet).apply { templateSetId = 10L } }

        service.createTemplateSet(
            1L,
            TemplateSetRequest(sortOrder = 0, targetReps = 8, targetRepsMax = null, setType = SetType.NORMAL),
            10L,
        )

        verify(templateSetRepository).save(any())
    }

    @Test
    fun `createTemplateSet propagates NotFoundException when the parent exercise isn't owned by the caller`() {
        whenever(ownershipResolver.getOwnedTemplateExercise(1L, 10L)).thenThrow(NotFoundException("TemplateExercise", 1L))

        assertThrows(NotFoundException::class.java) {
            service.createTemplateSet(1L, TemplateSetRequest(sortOrder = 0, setType = SetType.NORMAL), 10L)
        }
    }

    // ---- updateTemplateSet ----

    @Test
    fun `updateTemplateSet patches only the fields supplied`() {
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te, sortOrder = 0, targetReps = 8, setType = SetType.NORMAL)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        val result = service.updateTemplateSet(1L, TemplateSetPatchRequest(setType = SetType.WARMUP), 10L)

        assertEquals(SetType.WARMUP, result.setType)
        assertEquals(8, result.targetReps)
        assertEquals(0, result.sortOrder)
    }

    @Test
    fun `updateTemplateSet rejects a patched targetReps that would exceed the set's existing targetRepsMax`() {
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te, targetReps = 5, targetRepsMax = 10)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        val ex =
            assertThrows(BadRequestException::class.java) {
                service.updateTemplateSet(1L, TemplateSetPatchRequest(targetReps = 15), 10L)
            }
        assertEquals("targetReps must not exceed targetRepsMax", ex.message)
    }

    @Test
    fun `updateTemplateSet rejects a patched targetRepsMax that would fall below the set's existing targetReps`() {
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te, targetReps = 10, targetRepsMax = 12)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        val ex =
            assertThrows(BadRequestException::class.java) {
                service.updateTemplateSet(1L, TemplateSetPatchRequest(targetRepsMax = 5), 10L)
            }
        assertEquals("targetReps must not exceed targetRepsMax", ex.message)
    }

    @Test
    fun `updateTemplateSet accepts patching both targetReps and targetRepsMax together into a valid range`() {
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te, targetReps = 20, targetRepsMax = 25)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        val result =
            service.updateTemplateSet(
                1L,
                TemplateSetPatchRequest(targetReps = 5, targetRepsMax = 8),
                10L,
            )

        assertEquals(5, result.targetReps)
        assertEquals(8, result.targetRepsMax)
    }

    @Test
    fun `updateTemplateSet does not validate rep range when neither field is touched`() {
        // Existing data can already be inconsistent only if created before validation existed;
        // a patch that doesn't touch either field must not retroactively fail on old data.
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te, targetReps = 8, targetRepsMax = 8)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        val result = service.updateTemplateSet(1L, TemplateSetPatchRequest(sortOrder = 4), 10L)

        assertEquals(4, result.sortOrder)
    }

    @Test
    fun `updateTemplateSet propagates NotFoundException when not owned by the caller`() {
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenThrow(NotFoundException("TemplateSet", 1L))

        assertThrows(NotFoundException::class.java) {
            service.updateTemplateSet(1L, TemplateSetPatchRequest(sortOrder = 1), 10L)
        }
    }

    // ---- deleteTemplateSet ----

    @Test
    fun `deleteTemplateSet removes the caller's own set`() {
        val te = templateExercise(id = 1L)
        val ts = templateSet(parent = te)
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenReturn(ts)

        service.deleteTemplateSet(1L, 10L)

        verify(templateSetRepository).delete(ts)
    }

    @Test
    fun `deleteTemplateSet propagates NotFoundException and never calls repository delete`() {
        whenever(ownershipResolver.getOwnedTemplateSet(1L, 10L)).thenThrow(NotFoundException("TemplateSet", 1L))

        assertThrows(NotFoundException::class.java) { service.deleteTemplateSet(1L, 10L) }
        verify(templateSetRepository, never()).delete(any())
    }
}
