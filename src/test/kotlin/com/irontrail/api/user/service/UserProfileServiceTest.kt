package com.irontrail.api.user.service

import com.irontrail.api.common.ConflictException
import com.irontrail.api.common.NotFoundException
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.user.dto.UserProfilePatchRequest
import com.irontrail.api.user.dto.UserProfileRequest
import com.irontrail.api.user.model.Gender
import com.irontrail.api.user.model.MeasurementUnit
import com.irontrail.api.user.model.UserProfile
import com.irontrail.api.user.model.WeightUnit
import com.irontrail.api.user.repository.UserProfileRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
import java.util.Optional

class UserProfileServiceTest {
    private val userProfileRepository: UserProfileRepository = mock()
    private val splitRepository: SplitRepository = mock()

    private val userProfileService = UserProfileService(userProfileRepository, splitRepository)

    private fun savedProfile(
        userId: Long = 1L,
        name: String = "John",
        gender: Gender = Gender.MALE,
        weightUnit: WeightUnit = WeightUnit.KG,
        measurementUnit: MeasurementUnit = MeasurementUnit.METRIC,
        restTimerNotificationsEnabled: Boolean = true,
        activeSplitId: Long? = null,
    ) = UserProfile(
        name = name,
        gender = gender,
        weightUnit = weightUnit,
        measurementUnit = measurementUnit,
        restTimerNotificationsEnabled = restTimerNotificationsEnabled,
        profileImagePath = null,
        activeSplitId = activeSplitId,
    ).apply { this.userId = userId }

    private fun ownedSplit(
        splitId: Long = 10L,
        ownerId: Long = 1L,
    ) = Split(ownerId = ownerId, name = "Push Pull Legs").apply { this.splitId = splitId }

    // ---- create ----

    @Test
    fun `create saves a new profile for a user with no existing profile`() {
        whenever(userProfileRepository.existsById(1L)).thenReturn(false)
        val captor = argumentCaptor<UserProfile>()
        whenever(userProfileRepository.save(captor.capture())).thenAnswer { it.arguments[0] as UserProfile }

        val response =
            userProfileService.create(
                UserProfileRequest("John", Gender.MALE, WeightUnit.KG, MeasurementUnit.METRIC),
                userId = 1L,
            )

        assertEquals(1L, captor.firstValue.userId)
        assertEquals("John", captor.firstValue.name)
        assertEquals(Gender.MALE, captor.firstValue.gender)
        assertTrue(captor.firstValue.restTimerNotificationsEnabled)
        assertNull(captor.firstValue.profileImagePath)
        assertEquals(1L, response.userId)
        assertEquals("John", response.name)
    }

    @Test
    fun `create rejects a second profile for the same user with ConflictException, not a silent overwrite`() {
        whenever(userProfileRepository.existsById(1L)).thenReturn(true)

        assertThrows(ConflictException::class.java) {
            userProfileService.create(
                UserProfileRequest("John", Gender.MALE, WeightUnit.KG, MeasurementUnit.METRIC),
                userId = 1L,
            )
        }

        verify(userProfileRepository, never()).save(any())
    }

    @Test
    fun `create always starts a new profile with notifications enabled and no active split, regardless of request`() {
        whenever(userProfileRepository.existsById(2L)).thenReturn(false)
        val captor = argumentCaptor<UserProfile>()
        whenever(userProfileRepository.save(captor.capture())).thenAnswer { it.arguments[0] as UserProfile }

        userProfileService.create(
            UserProfileRequest("Jane", Gender.FEMALE, WeightUnit.LBS, MeasurementUnit.IMPERIAL),
            userId = 2L,
        )

        assertTrue(captor.firstValue.restTimerNotificationsEnabled)
        assertNull(captor.firstValue.activeSplitId)
    }

    // ---- findByUserId ----

    @Test
    fun `findByUserId returns the caller's own profile, scoped by the caller's id`() {
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(savedProfile(userId = 1L, name = "John")))

        val response = userProfileService.findByUserId(1L)

        assertEquals(1L, response.userId)
        assertEquals("John", response.name)
    }

    @Test
    fun `findByUserId throws NotFoundException instead of returning null when caller has no profile yet`() {
        whenever(userProfileRepository.findById(42L)).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            userProfileService.findByUserId(42L)
        }
    }

    // ---- update: not-found ----

    @Test
    fun `update throws NotFoundException when the caller has no profile to patch`() {
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.empty())

        assertThrows(NotFoundException::class.java) {
            userProfileService.update(UserProfilePatchRequest(name = "New Name"), userId = 1L)
        }
    }

    // ---- update: merge semantics, omitted fields unchanged ----

    @Test
    fun `update with an entirely empty patch leaves every field unchanged`() {
        val profile =
            savedProfile(
                name = "John",
                gender = Gender.MALE,
                weightUnit = WeightUnit.KG,
                measurementUnit = MeasurementUnit.METRIC,
                restTimerNotificationsEnabled = true,
                activeSplitId = 10L,
            )
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(), userId = 1L)

        assertEquals("John", response.name)
        assertEquals(Gender.MALE, response.gender)
        assertEquals(WeightUnit.KG, response.weightUnit)
        assertEquals(MeasurementUnit.METRIC, response.measurementUnit)
        assertTrue(response.restTimerNotificationsEnabled)
        assertEquals(10L, response.activeSplitId)
    }

    @Test
    fun `update only changes the single field supplied, leaving siblings untouched`() {
        val profile = savedProfile(name = "John", gender = Gender.MALE, weightUnit = WeightUnit.KG)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(name = "Jonathan"), userId = 1L)

        assertEquals("Jonathan", response.name)
        assertEquals(Gender.MALE, response.gender)
        assertEquals(WeightUnit.KG, response.weightUnit)
    }

    @Test
    fun `update patches gender when supplied`() {
        val profile = savedProfile(gender = Gender.MALE)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(gender = Gender.FEMALE), userId = 1L)

        assertEquals(Gender.FEMALE, response.gender)
    }

    @Test
    fun `update patches weightUnit when supplied`() {
        val profile = savedProfile(weightUnit = WeightUnit.KG)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(weightUnit = WeightUnit.LBS), userId = 1L)

        assertEquals(WeightUnit.LBS, response.weightUnit)
    }

    @Test
    fun `update patches measurementUnit when supplied`() {
        val profile = savedProfile(measurementUnit = MeasurementUnit.METRIC)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(measurementUnit = MeasurementUnit.IMPERIAL), userId = 1L)

        assertEquals(MeasurementUnit.IMPERIAL, response.measurementUnit)
    }

    // ---- update: boolean false must be distinguished from absent ----

    @Test
    fun `update sets restTimerNotificationsEnabled from true to false when explicitly sent as false, not treated as absent`() {
        val profile = savedProfile(restTimerNotificationsEnabled = true)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response =
            userProfileService.update(
                UserProfilePatchRequest(restTimerNotificationsEnabled = false),
                userId = 1L,
            )

        assertFalse(response.restTimerNotificationsEnabled)
    }

    @Test
    fun `update leaves restTimerNotificationsEnabled unchanged when omitted, even though false is a valid value`() {
        val profile = savedProfile(restTimerNotificationsEnabled = false)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(name = "Someone Else"), userId = 1L)

        assertFalse(response.restTimerNotificationsEnabled)
    }

    @Test
    fun `update flips restTimerNotificationsEnabled back to true when explicitly sent as true`() {
        val profile = savedProfile(restTimerNotificationsEnabled = false)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response =
            userProfileService.update(
                UserProfilePatchRequest(restTimerNotificationsEnabled = true),
                userId = 1L,
            )

        assertTrue(response.restTimerNotificationsEnabled)
    }

    // ---- update: activeSplitId ownership validation ----

    @Test
    fun `update accepts an activeSplitId the caller actually owns`() {
        val profile = savedProfile(activeSplitId = null)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))
        whenever(splitRepository.findBySplitIdAndOwnerId(10L, 1L)).thenReturn(ownedSplit(splitId = 10L, ownerId = 1L))

        val response = userProfileService.update(UserProfilePatchRequest(activeSplitId = 10L), userId = 1L)

        assertEquals(10L, response.activeSplitId)
    }

    @Test
    fun `update rejects an activeSplitId owned by a different user with NotFoundException, not silently accepted`() {
        val profile = savedProfile(activeSplitId = null)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))
        whenever(splitRepository.findBySplitIdAndOwnerId(10L, 1L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            userProfileService.update(UserProfilePatchRequest(activeSplitId = 10L), userId = 1L)
        }

        assertNull(profile.activeSplitId)
    }

    @Test
    fun `update rejects an activeSplitId that doesn't exist at all with NotFoundException`() {
        val profile = savedProfile(activeSplitId = null)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))
        whenever(splitRepository.findBySplitIdAndOwnerId(999L, 1L)).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            userProfileService.update(UserProfilePatchRequest(activeSplitId = 999L), userId = 1L)
        }
    }

    @Test
    fun `update does not touch activeSplitId or call splitRepository when activeSplitId is omitted`() {
        val profile = savedProfile(activeSplitId = 10L)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(name = "New Name"), userId = 1L)

        assertEquals(10L, response.activeSplitId)
        verify(splitRepository, never()).findBySplitIdAndOwnerId(any(), any())
    }

    @Test
    fun `update cannot clear an already-set activeSplitId back to null - a known merge-patch limitation, not a bug`() {
        // request.activeSplitId is a nullable Long with no way to distinguish "absent" from "explicit null" in this
        // codebase's merge-patch approach, so activeSplitId == null in the DTO is always treated as "leave unchanged."
        val profile = savedProfile(activeSplitId = 10L)
        whenever(userProfileRepository.findById(1L)).thenReturn(Optional.of(profile))

        val response = userProfileService.update(UserProfilePatchRequest(activeSplitId = null), userId = 1L)

        assertEquals(10L, response.activeSplitId)
        verify(splitRepository, never()).findBySplitIdAndOwnerId(any(), any())
    }

    // ---- update: caller scoping ----

    @Test
    fun `update looks up the profile by the caller's id, never a client-supplied id`() {
        val profile = savedProfile(userId = 7L)
        whenever(userProfileRepository.findById(7L)).thenReturn(Optional.of(profile))

        userProfileService.update(UserProfilePatchRequest(name = "Renamed"), userId = 7L)

        verify(userProfileRepository).findById(7L)
    }

    @Test
    fun `activeSplitId ownership check is scoped to the caller's id, not the split owner recorded elsewhere`() {
        val profile = savedProfile(userId = 3L, activeSplitId = null)
        whenever(userProfileRepository.findById(3L)).thenReturn(Optional.of(profile))
        whenever(splitRepository.findBySplitIdAndOwnerId(10L, 3L)).thenReturn(ownedSplit(splitId = 10L, ownerId = 3L))

        userProfileService.update(UserProfilePatchRequest(activeSplitId = 10L), userId = 3L)

        verify(splitRepository).findBySplitIdAndOwnerId(10L, 3L)
    }
}
