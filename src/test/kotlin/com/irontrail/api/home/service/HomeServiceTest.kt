package com.irontrail.api.home.service

import com.irontrail.api.session.dto.toWorkoutSessionResponse
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.WorkoutSessionRepository
import com.irontrail.api.split.model.Split
import com.irontrail.api.split.model.TemplateExercise
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.TemplateExerciseRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import com.irontrail.api.user.model.Gender
import com.irontrail.api.user.model.MeasurementUnit
import com.irontrail.api.user.model.UserProfile
import com.irontrail.api.user.model.WeightUnit
import com.irontrail.api.user.repository.UserProfileRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.Optional

class HomeServiceTest {
    private val userProfileRepository: UserProfileRepository = mock()
    private val splitRepository: SplitRepository = mock()
    private val workoutDayRepository: WorkoutDayRepository = mock()
    private val templateExerciseRepository: TemplateExerciseRepository = mock()
    private val workoutSessionRepository: WorkoutSessionRepository = mock()

    private val homeService =
        HomeService(
            userProfileRepository,
            splitRepository,
            workoutDayRepository,
            templateExerciseRepository,
            workoutSessionRepository,
        )

    private val userId = 1L

    // ---- test data builders ----

    private fun profile(
        id: Long = userId,
        activeSplitId: Long? = null,
    ): UserProfile =
        UserProfile(
            name = "Test User",
            gender = Gender.PREFER_NOT_TO_SAY,
            weightUnit = WeightUnit.KG,
            measurementUnit = MeasurementUnit.METRIC,
            restTimerNotificationsEnabled = true,
            profileImagePath = null,
            activeSplitId = activeSplitId,
        ).apply { this.userId = id }

    private fun split(
        id: Long = 10L,
        ownerId: Long = userId,
        name: String = "Push Pull Legs",
    ): Split = Split(ownerId = ownerId, name = name).apply { splitId = id }

    private fun workoutDay(
        id: Long,
        splitId: Long = 10L,
        name: String,
        sortOrder: Int,
    ): WorkoutDay = WorkoutDay(splitId = splitId, name = name, sortOrder = sortOrder).apply { workoutDayId = id }

    private fun templateExercise(
        id: Long,
        workoutDayId: Long,
    ): TemplateExercise = TemplateExercise(workoutDayId = workoutDayId, exerciseId = 1L, sortOrder = 0).apply { templateExerciseId = id }

    private fun session(
        id: Long = 1L,
        ownerId: Long = userId,
        workoutDayId: Long? = null,
        startedAt: OffsetDateTime,
        status: SessionStatus = SessionStatus.COMPLETED,
        splitNameSnapshot: String? = null,
        workoutDayNameSnapshot: String? = null,
    ): WorkoutSession =
        WorkoutSession(
            ownerId = ownerId,
            workoutDayId = workoutDayId,
            splitNameSnapshot = splitNameSnapshot,
            workoutDayNameSnapshot = workoutDayNameSnapshot,
            startedAt = startedAt,
            durationSeconds = 1_800L,
            status = status,
        ).apply { sessionId = id }

    // ---- stub helpers ----

    private fun stubEmptyProfile() = whenever(userProfileRepository.findById(userId)).thenReturn(Optional.empty())

    private fun stubProfile(profile: UserProfile) = whenever(userProfileRepository.findById(userId)).thenReturn(Optional.of(profile))

    /** Stubs both "history" queries (month/streak window + recent-3) to look like a user with zero completed sessions. */
    private fun stubEmptyHistory() {
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(emptyList())
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())
    }

    private fun currentWeekMonday(): LocalDate = LocalDate.now().with(DayOfWeek.MONDAY)

    private fun dateTimeInWeek(
        monday: LocalDate,
        daysAfterMonday: Long = 0,
    ): OffsetDateTime = monday.plusDays(daysAfterMonday).atTime(9, 0).atOffset(OffsetDateTime.now().offset)

    // ---- empty overall state ----

    @Test
    fun `getHome returns a fully empty response for a brand new user with no profile, split, or sessions`() {
        stubEmptyProfile()
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertNull(response.nextWorkout)
        assertTrue(response.trainedDatesThisMonth.isEmpty())
        assertEquals(0, response.workoutsThisMonth)
        assertEquals(0, response.weekStreak)
        assertTrue(response.recentWorkouts.isEmpty())
        verifyNoInteractions(splitRepository)
        verifyNoInteractions(workoutDayRepository)
        verifyNoInteractions(templateExerciseRepository)
    }

    // ---- next-workout suggestion ----

    @Test
    fun `getHome does not query splits when the user has no profile yet`() {
        stubEmptyProfile()
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertNull(response.nextWorkout)
        verifyNoInteractions(splitRepository)
    }

    @Test
    fun `getHome does not query splits when the profile has no active split set`() {
        stubProfile(profile(activeSplitId = null))
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertNull(response.nextWorkout)
        verifyNoInteractions(splitRepository)
    }

    @Test
    fun `getHome returns null nextWorkout when the profile's active split no longer exists`() {
        stubProfile(profile(activeSplitId = 99L))
        whenever(splitRepository.findById(99L)).thenReturn(Optional.empty())
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertNull(response.nextWorkout)
        verifyNoInteractions(workoutDayRepository)
    }

    @Test
    fun `getHome returns null nextWorkout when the active split has zero workout days`() {
        stubProfile(profile(activeSplitId = 10L))
        whenever(splitRepository.findById(10L)).thenReturn(Optional.of(split()))
        whenever(workoutDayRepository.findBySplitIdIn(listOf(10L))).thenReturn(emptyList())
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertNull(response.nextWorkout)
        verifyNoInteractions(templateExerciseRepository)
        verify(workoutSessionRepository, never())
            .findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(any(), any(), any())
    }

    @Test
    fun `getHome suggests the first workout day by sortOrder when the split has no completed sessions yet`() {
        stubProfile(profile(activeSplitId = 10L))
        whenever(splitRepository.findById(10L)).thenReturn(Optional.of(split(name = "Push Pull Legs")))
        // Deliberately out of sortOrder to prove the service sorts them itself, not the repository.
        val days =
            listOf(
                workoutDay(id = 3, name = "Legs", sortOrder = 2),
                workoutDay(id = 1, name = "Push", sortOrder = 0),
                workoutDay(id = 2, name = "Pull", sortOrder = 1),
            )
        whenever(workoutDayRepository.findBySplitIdIn(listOf(10L))).thenReturn(days)
        whenever(
            workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
                eq(userId),
                any(),
                eq(SessionStatus.COMPLETED),
            ),
        ).thenReturn(null)
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L)))
            .thenReturn(listOf(templateExercise(100, 1)))
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertEquals(1L, response.nextWorkout?.workoutDayId)
        assertEquals("Push", response.nextWorkout?.workoutDayName)
        assertEquals("Push Pull Legs", response.nextWorkout?.splitName)
        assertEquals(1, response.nextWorkout?.exerciseCount)
        verify(templateExerciseRepository).findByWorkoutDayIdIn(listOf(1L))
    }

    @Test
    fun `getHome suggests the next workout day by sortOrder after the most recently completed day`() {
        stubProfile(profile(activeSplitId = 10L))
        whenever(splitRepository.findById(10L)).thenReturn(Optional.of(split()))
        val push = workoutDay(id = 1, name = "Push", sortOrder = 0)
        val pull = workoutDay(id = 2, name = "Pull", sortOrder = 1)
        val legs = workoutDay(id = 3, name = "Legs", sortOrder = 2)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(10L))).thenReturn(listOf(push, pull, legs))
        whenever(
            workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
                eq(userId),
                any(),
                eq(SessionStatus.COMPLETED),
            ),
        ).thenReturn(session(id = 50, workoutDayId = 1L, startedAt = OffsetDateTime.now().minusDays(1)))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(2L))).thenReturn(emptyList())
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertEquals(2L, response.nextWorkout?.workoutDayId)
        assertEquals("Pull", response.nextWorkout?.workoutDayName)
    }

    @Test
    fun `getHome wraps around to the first workout day after completing the last day in the split`() {
        stubProfile(profile(activeSplitId = 10L))
        whenever(splitRepository.findById(10L)).thenReturn(Optional.of(split()))
        val push = workoutDay(id = 1, name = "Push", sortOrder = 0)
        val pull = workoutDay(id = 2, name = "Pull", sortOrder = 1)
        val legs = workoutDay(id = 3, name = "Legs", sortOrder = 2)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(10L))).thenReturn(listOf(push, pull, legs))
        // Most recently completed day is the LAST day (highest sortOrder) in the split.
        whenever(
            workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
                eq(userId),
                any(),
                eq(SessionStatus.COMPLETED),
            ),
        ).thenReturn(session(id = 51, workoutDayId = 3L, startedAt = OffsetDateTime.now().minusDays(1)))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertEquals(1L, response.nextWorkout?.workoutDayId)
        assertEquals("Push", response.nextWorkout?.workoutDayName)
    }

    @Test
    fun `getHome falls back to the first workout day when the most recently completed session's day is no longer in the split`() {
        stubProfile(profile(activeSplitId = 10L))
        whenever(splitRepository.findById(10L)).thenReturn(Optional.of(split()))
        val push = workoutDay(id = 1, name = "Push", sortOrder = 0)
        val pull = workoutDay(id = 2, name = "Pull", sortOrder = 1)
        whenever(workoutDayRepository.findBySplitIdIn(listOf(10L))).thenReturn(listOf(push, pull))
        // lastCompleted references a workoutDayId that no longer exists among the split's current days
        // (e.g. the day was deleted after the session was logged).
        whenever(
            workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
                eq(userId),
                any(),
                eq(SessionStatus.COMPLETED),
            ),
        ).thenReturn(session(id = 52, workoutDayId = 999L, startedAt = OffsetDateTime.now().minusDays(1)))
        whenever(templateExerciseRepository.findByWorkoutDayIdIn(listOf(1L))).thenReturn(emptyList())
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertEquals(1L, response.nextWorkout?.workoutDayId)
        assertEquals("Push", response.nextWorkout?.workoutDayName)
    }

    // ---- month stats ----

    @Test
    fun `getHome counts every completed session toward workoutsThisMonth but dedupes trainedDatesThisMonth by calendar day`() {
        stubEmptyProfile()
        val day = OffsetDateTime.now().withDayOfMonth(1).plusDays(4) // the 5th of the current month, always a valid date
        val sessions =
            listOf(
                session(id = 1, startedAt = day.withHour(8)),
                session(id = 2, startedAt = day.withHour(18)),
            )
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(2, response.workoutsThisMonth)
        assertEquals(1, response.trainedDatesThisMonth.size)
        assertEquals(day.toLocalDate(), response.trainedDatesThisMonth.first())
    }

    @Test
    fun `getHome returns trainedDatesThisMonth sorted ascending regardless of repository return order`() {
        stubEmptyProfile()
        val base = OffsetDateTime.now().withDayOfMonth(1)
        val late = session(id = 1, startedAt = base.plusDays(20))
        val early = session(id = 2, startedAt = base.plusDays(2))
        val mid = session(id = 3, startedAt = base.plusDays(10))
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(listOf(late, early, mid))
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(
            listOf(early.startedAt.toLocalDate(), mid.startedAt.toLocalDate(), late.startedAt.toLocalDate()),
            response.trainedDatesThisMonth,
        )
    }

    @Test
    fun `getHome queries month sessions from the first instant of the current month and streak sessions from two years back`() {
        stubEmptyProfile()
        val expectedStartOfMonth =
            OffsetDateTime
                .now()
                .withDayOfMonth(1)
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
        val expectedStreakLowerBound = OffsetDateTime.now().minusYears(2)
        val currentOffset = OffsetDateTime.now().offset
        stubEmptyHistory()

        homeService.getHome(userId)

        val captor = argumentCaptor<OffsetDateTime>()
        verify(workoutSessionRepository, times(2))
            .findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), captor.capture())

        val (monthAfter, streakAfter) = captor.allValues
        assertEquals(expectedStartOfMonth.toLocalDate(), monthAfter.toLocalDate())
        assertEquals(0, monthAfter.hour)
        assertEquals(0, monthAfter.minute)
        assertEquals(0, monthAfter.second)
        assertEquals(0, monthAfter.nano)
        // The month boundary is computed in the server's own current offset, not normalized to UTC.
        assertEquals(currentOffset, monthAfter.offset)

        val diff = Duration.between(expectedStreakLowerBound, streakAfter).abs()
        assertTrue(diff.seconds < 10, "expected streak lower bound close to now-2y, was off by ${diff.seconds}s")
    }

    // ---- week streak ----

    @Test
    fun `getHome returns a week streak of 0 when the user has never completed a session`() {
        stubEmptyProfile()
        stubEmptyHistory()

        val response = homeService.getHome(userId)

        assertEquals(0, response.weekStreak)
    }

    @Test
    fun `getHome returns a streak of 1 for a single completed session in the current, still-in-progress week`() {
        stubEmptyProfile()
        val monday = currentWeekMonday()
        val sessions = listOf(session(id = 1, startedAt = dateTimeInWeek(monday)))
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(1, response.weekStreak)
    }

    @Test
    fun `getHome counts 3 consecutive weeks including the current week`() {
        stubEmptyProfile()
        val monday = currentWeekMonday()
        val sessions =
            listOf(
                session(id = 1, startedAt = dateTimeInWeek(monday)),
                session(id = 2, startedAt = dateTimeInWeek(monday.minusWeeks(1), 2)),
                session(id = 3, startedAt = dateTimeInWeek(monday.minusWeeks(2), 4)),
            )
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(3, response.weekStreak)
    }

    @Test
    fun `getHome does not break the streak when the current week has no session yet - only a fully-elapsed empty week does`() {
        stubEmptyProfile()
        val monday = currentWeekMonday()
        // No session in the current week; sessions in the two prior weeks are still consecutive.
        val sessions =
            listOf(
                session(id = 1, startedAt = dateTimeInWeek(monday.minusWeeks(1), 1)),
                session(id = 2, startedAt = dateTimeInWeek(monday.minusWeeks(2), 3)),
            )
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(2, response.weekStreak)
    }

    @Test
    fun `getHome stops the streak at a genuine gap week instead of counting through it`() {
        stubEmptyProfile()
        val monday = currentWeekMonday()
        // Session this week and 2 weeks ago, but NOT 1 week ago - a real gap.
        val sessions =
            listOf(
                session(id = 1, startedAt = dateTimeInWeek(monday)),
                session(id = 2, startedAt = dateTimeInWeek(monday.minusWeeks(2), 3)),
            )
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        assertEquals(1, response.weekStreak)
    }

    @Test
    fun `getHome counts a week only once even with multiple completed sessions logged that week`() {
        stubEmptyProfile()
        val monday = currentWeekMonday()
        val sessions =
            listOf(
                session(id = 1, startedAt = dateTimeInWeek(monday, 0)),
                session(id = 2, startedAt = dateTimeInWeek(monday, 3)), // same week, different day
                session(id = 3, startedAt = dateTimeInWeek(monday.minusWeeks(1), 1)),
            )
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(sessions)
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(emptyList())

        val response = homeService.getHome(userId)

        // 2 distinct weeks, not 3 sessions.
        assertEquals(2, response.weekStreak)
    }

    @Test
    fun `getHome requests only COMPLETED sessions from the repository for month stats and streak`() {
        stubEmptyProfile()
        stubEmptyHistory()

        homeService.getHome(userId)

        verify(workoutSessionRepository, times(2))
            .findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any())
        verify(workoutSessionRepository, never())
            .findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.ACTIVE), any())
        verify(workoutSessionRepository, never())
            .findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.PAUSED), any())
    }

    // ---- recent workouts ----

    @Test
    fun `getHome returns fewer than 3 recent workouts when the user has fewer completed sessions`() {
        stubEmptyProfile()
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(emptyList())
        val sessions =
            listOf(
                session(id = 1, startedAt = OffsetDateTime.now().minusDays(1)),
                session(id = 2, startedAt = OffsetDateTime.now().minusDays(3)),
            )
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(sessions)

        val response = homeService.getHome(userId)

        assertEquals(2, response.recentWorkouts.size)
        assertEquals(listOf(1L, 2L), response.recentWorkouts.map { it.sessionId })
    }

    @Test
    fun `getHome preserves the most-recent-first order returned by the repository for recent workouts`() {
        stubEmptyProfile()
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(emptyList())
        val sessions =
            listOf(
                session(id = 3, startedAt = OffsetDateTime.now().minusDays(1)),
                session(id = 2, startedAt = OffsetDateTime.now().minusDays(2)),
                session(id = 1, startedAt = OffsetDateTime.now().minusDays(3)),
            )
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(sessions)

        val response = homeService.getHome(userId)

        assertEquals(listOf(3L, 2L, 1L), response.recentWorkouts.map { it.sessionId })
    }

    @Test
    fun `getHome maps each recent WorkoutSession to its response via the shared session-to-response mapping`() {
        stubEmptyProfile()
        whenever(workoutSessionRepository.findByOwnerIdAndStatusAndStartedAtAfter(eq(userId), eq(SessionStatus.COMPLETED), any()))
            .thenReturn(emptyList())
        val detailed =
            session(
                id = 7,
                workoutDayId = 3L,
                startedAt = OffsetDateTime.now().minusDays(2),
                splitNameSnapshot = "PPL",
                workoutDayNameSnapshot = "Push",
            ).apply {
                durationSeconds = 3_600L
                totalVolumeKg = 1234.5
                completedSets = 10
                totalSets = 12
                notes = "felt strong"
            }
        whenever(workoutSessionRepository.findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(eq(userId), eq(SessionStatus.COMPLETED)))
            .thenReturn(listOf(detailed))

        val response = homeService.getHome(userId)

        assertEquals(detailed.toWorkoutSessionResponse(), response.recentWorkouts.single())
    }
}
