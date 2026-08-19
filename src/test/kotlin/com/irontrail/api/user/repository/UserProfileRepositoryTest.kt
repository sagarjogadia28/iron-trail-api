package com.irontrail.api.user.repository

import com.irontrail.api.testsupport.RepositoryTestBase
import com.irontrail.api.user.model.Gender
import com.irontrail.api.user.model.MeasurementUnit
import com.irontrail.api.user.model.User
import com.irontrail.api.user.model.UserProfile
import com.irontrail.api.user.model.WeightUnit
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager

// Regression coverage for a real bug caught in the 2026-08-18 spec/mockup audit: this repository
// was declared as JpaRepository<User, Long> instead of JpaRepository<UserProfile, Long>. If that
// wrong type were still in place, this file wouldn't compile - save()/findById() below require the
// declared generic type to actually be UserProfile.
class UserProfileRepositoryTest : RepositoryTestBase() {

    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userProfileRepository: UserProfileRepository

    private fun persistUser(): Long =
        entityManager.persistAndFlush(User(email = "user-${System.nanoTime()}@test.com", passwordHash = "hash")).userId

    private fun profile(userId: Long, name: String = "Sagar") = UserProfile(
        name = name,
        gender = Gender.MALE,
        weightUnit = WeightUnit.KG,
        measurementUnit = MeasurementUnit.METRIC,
        restTimerNotificationsEnabled = true,
        profileImagePath = null
    ).apply { this.userId = userId }

    @Test
    fun `save then findById round-trips a profile sharing the user's own id as its primary key`() {
        val userId = persistUser()

        val saved = userProfileRepository.save(profile(userId, name = "Sagar Jogadia"))
        val found = userProfileRepository.findById(userId)

        assertTrue(found.isPresent)
        assertEquals(userId, saved.userId)
        assertEquals("Sagar Jogadia", found.get().name)
        assertEquals(Gender.MALE, found.get().gender)
    }

    @Test
    fun `findById returns empty for a user with no profile yet`() {
        val userId = persistUser()

        assertTrue(userProfileRepository.findById(userId).isEmpty)
    }

    @Test
    fun `existsById is true only after a profile has been created for that user`() {
        val userId = persistUser()
        assertFalse(userProfileRepository.existsById(userId))

        userProfileRepository.save(profile(userId))

        assertTrue(userProfileRepository.existsById(userId))
    }

    @Test
    fun `deleting a profile does not delete the underlying user - the FK direction is profile depends on user`() {
        val userId = persistUser()
        val saved = userProfileRepository.save(profile(userId))

        userProfileRepository.delete(saved)

        assertFalse(userProfileRepository.existsById(userId))
        assertTrue(entityManager.find(User::class.java, userId) != null)
    }
}
