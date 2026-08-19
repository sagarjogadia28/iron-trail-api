package com.irontrail.api.split.repository

import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

class WorkoutDayRepositoryTest : RepositoryTestBase() {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var workoutDayRepository: WorkoutDayRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistSplit(ownerId: Long, name: String = "PPL"): Split =
        entityManager.persistAndFlush(Split(ownerId = ownerId, name = name))

    private fun persistDay(splitId: Long, name: String = "Push Day", sortOrder: Int = 0): WorkoutDay =
        entityManager.persistAndFlush(WorkoutDay(splitId = splitId, name = name, sortOrder = sortOrder))

    // ---- findBySplitIdIn ----

    @Test
    fun `findBySplitIdIn returns days for every requested split id`() {
        val owner = persistUser()
        val splitA = persistSplit(owner, "A")
        val splitB = persistSplit(owner, "B")
        persistDay(splitA.splitId, "Day A1")
        persistDay(splitB.splitId, "Day B1")

        val result = workoutDayRepository.findBySplitIdIn(listOf(splitA.splitId, splitB.splitId))

        assertEquals(setOf("Day A1", "Day B1"), result.map { it.name }.toSet())
    }

    @Test
    fun `findBySplitIdIn excludes days belonging to a split not in the requested ids`() {
        val owner = persistUser()
        val splitA = persistSplit(owner, "A")
        val splitB = persistSplit(owner, "B")
        persistDay(splitA.splitId, "Included")
        persistDay(splitB.splitId, "Excluded")

        val result = workoutDayRepository.findBySplitIdIn(listOf(splitA.splitId))

        assertEquals(listOf("Included"), result.map { it.name })
    }

    @Test
    fun `findBySplitIdIn returns an empty list for an empty id list`() {
        val result = workoutDayRepository.findBySplitIdIn(emptyList())

        assertTrue(result.isEmpty())
    }

    // ---- findOwnedByWorkoutDayId ----

    @Test
    fun `findOwnedByWorkoutDayId returns the day when its split is owned by the caller`() {
        val owner = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId, "Push Day")

        val result = workoutDayRepository.findOwnedByWorkoutDayId(day.workoutDayId, owner)

        assertEquals("Push Day", result?.name)
    }

    @Test
    fun `findOwnedByWorkoutDayId returns null when the parent split is owned by someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val split = persistSplit(owner)
        val day = persistDay(split.splitId)

        val result = workoutDayRepository.findOwnedByWorkoutDayId(day.workoutDayId, stranger)

        assertNull(result)
    }

    @Test
    fun `findOwnedByWorkoutDayId returns null for a non-existent day id`() {
        val owner = persistUser()

        val result = workoutDayRepository.findOwnedByWorkoutDayId(999_999L, owner)

        assertNull(result)
    }
}
