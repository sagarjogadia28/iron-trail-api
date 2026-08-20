package com.irontrail.api.split.repository

import com.irontrail.api.split.model.Split
import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

class SplitRepositoryTest : RepositoryTestBase() {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var splitRepository: SplitRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun persistSplit(
        ownerId: Long,
        name: String = "PPL",
    ): Split = entityManager.persistAndFlush(Split(ownerId = ownerId, name = name))

    // ---- findByOwnerId ----

    @Test
    fun `findByOwnerId returns only splits owned by that user`() {
        val owner = persistUser()
        val stranger = persistUser()
        persistSplit(owner, "Mine")
        persistSplit(stranger, "Theirs")

        val result = splitRepository.findByOwnerId(owner)

        assertEquals(listOf("Mine"), result.map { it.name })
    }

    @Test
    fun `findByOwnerId returns an empty list for a user with no splits`() {
        val owner = persistUser()

        val result = splitRepository.findByOwnerId(owner)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `findByOwnerId returns every split when the owner has more than one`() {
        val owner = persistUser()
        persistSplit(owner, "Push Pull Legs")
        persistSplit(owner, "Upper Lower")

        val result = splitRepository.findByOwnerId(owner)

        assertEquals(setOf("Push Pull Legs", "Upper Lower"), result.map { it.name }.toSet())
    }

    // ---- findBySplitIdAndOwnerId ----

    @Test
    fun `findBySplitIdAndOwnerId returns the split when owned by that exact user`() {
        val owner = persistUser()
        val split = persistSplit(owner, "PPL")

        val result = splitRepository.findBySplitIdAndOwnerId(split.splitId, owner)

        assertEquals("PPL", result?.name)
    }

    @Test
    fun `findBySplitIdAndOwnerId returns null when the split is owned by someone else`() {
        val owner = persistUser()
        val stranger = persistUser()
        val split = persistSplit(owner, "PPL")

        val result = splitRepository.findBySplitIdAndOwnerId(split.splitId, stranger)

        assertNull(result)
    }

    @Test
    fun `findBySplitIdAndOwnerId returns null for a non-existent split id`() {
        val owner = persistUser()

        val result = splitRepository.findBySplitIdAndOwnerId(999_999L, owner)

        assertNull(result)
    }
}
