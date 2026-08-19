package com.irontrail.api.user.repository

import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.User
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

class UserRepositoryTest : RepositoryTestBase() {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userRepository: UserRepository

    // ---- findByEmail ----

    @Test
    fun `findByEmail returns the user for an exact match`() {
        entityManager.persistAndFlush(User(email = "sagar@test.com", passwordHash = "hash"))

        val result = userRepository.findByEmail("sagar@test.com")

        assertEquals("sagar@test.com", result?.email)
    }

    @Test
    fun `findByEmail returns null when no user has that email`() {
        val result = userRepository.findByEmail("nobody@test.com")

        assertNull(result)
    }

    @Test
    fun `findByEmail is case-sensitive at the query level - normalization is the caller's responsibility`() {
        // AuthService normalizes (.trim().lowercase()) before ever calling this method - this test
        // documents why that normalization has to happen there rather than being assumed here.
        entityManager.persistAndFlush(User(email = "sagar@test.com", passwordHash = "hash"))

        val result = userRepository.findByEmail("Sagar@Test.com")

        assertNull(result)
    }
}
