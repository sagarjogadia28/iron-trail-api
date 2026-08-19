package com.irontrail.api.home.service

import com.irontrail.api.home.dto.HomeResponse
import com.irontrail.api.home.dto.NextWorkoutResponse
import com.irontrail.api.session.dto.toWorkoutSessionResponse
import com.irontrail.api.session.model.SessionStatus
import com.irontrail.api.session.model.WorkoutSession
import com.irontrail.api.session.repository.WorkoutSessionRepository
import com.irontrail.api.split.model.WorkoutDay
import com.irontrail.api.split.repository.SplitRepository
import com.irontrail.api.split.repository.TemplateExerciseRepository
import com.irontrail.api.split.repository.WorkoutDayRepository
import com.irontrail.api.user.repository.UserProfileRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
@Transactional
class HomeService(
    private val userProfileRepository: UserProfileRepository,
    private val splitRepository: SplitRepository,
    private val workoutDayRepository: WorkoutDayRepository,
    private val templateExerciseRepository: TemplateExerciseRepository,
    private val workoutSessionRepository: WorkoutSessionRepository
) {
    fun getHome(userId: Long): HomeResponse {
        val activeSplitId = userProfileRepository.findById(userId).orElse(null)?.activeSplitId
        val nextWorkout = activeSplitId?.let { buildNextWorkout(it, userId) }

        val now = OffsetDateTime.now()
        val startOfMonth = now.withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0).withNano(0)
        val monthSessions = workoutSessionRepository
            .findByOwnerIdAndStatusAndStartedAtAfter(userId, SessionStatus.COMPLETED, startOfMonth)
        val trainedDates = monthSessions.map { it.startedAt.toLocalDate() }.distinct().sorted()

        val streakSessions = workoutSessionRepository
            .findByOwnerIdAndStatusAndStartedAtAfter(userId, SessionStatus.COMPLETED, now.minusYears(2))
        val weekStreak = computeWeekStreak(streakSessions.map { it.startedAt.toLocalDate() }.toSet())

        val recentWorkouts = workoutSessionRepository
            .findTop3ByOwnerIdAndStatusOrderByStartedAtDesc(userId, SessionStatus.COMPLETED)
            .map { it.toWorkoutSessionResponse() }

        return HomeResponse(
            nextWorkout = nextWorkout,
            trainedDatesThisMonth = trainedDates,
            workoutsThisMonth = monthSessions.size,
            weekStreak = weekStreak,
            recentWorkouts = recentWorkouts
        )
    }

    private fun buildNextWorkout(splitId: Long, userId: Long): NextWorkoutResponse? {
        val split = splitRepository.findById(splitId).orElse(null) ?: return null
        val days = workoutDayRepository.findBySplitIdIn(listOf(splitId)).sortedBy { it.sortOrder }
        if (days.isEmpty()) return null

        val lastCompleted = workoutSessionRepository.findTopByOwnerIdAndWorkoutDayIdInAndStatusOrderByStartedAtDesc(
            userId, days.map { it.workoutDayId }, SessionStatus.COMPLETED
        )
        val nextDay = nextDayAfter(days, lastCompleted)
        val exerciseCount = templateExerciseRepository.findByWorkoutDayIdIn(listOf(nextDay.workoutDayId)).size

        return NextWorkoutResponse(
            workoutDayId = nextDay.workoutDayId,
            workoutDayName = nextDay.name,
            splitName = split.name,
            exerciseCount = exerciseCount
        )
    }

    private fun nextDayAfter(days: List<WorkoutDay>, lastCompleted: WorkoutSession?): WorkoutDay {
        if (lastCompleted == null) return days.first()
        val lastIndex = days.indexOfFirst { it.workoutDayId == lastCompleted.workoutDayId }
        if (lastIndex == -1) return days.first()
        return days[(lastIndex + 1) % days.size]
    }

    private fun computeWeekStreak(sessionDates: Set<LocalDate>): Int {
        val weeksWithWorkout = sessionDates.map { it.with(DayOfWeek.MONDAY) }.toSet()
        var streak = 0
        var weekStart = LocalDate.now().with(DayOfWeek.MONDAY)
        var isCurrentWeek = true
        while (true) {
            if (weekStart in weeksWithWorkout) {
                streak++
                isCurrentWeek = false
            } else if (isCurrentWeek) {
                isCurrentWeek = false
            } else {
                break
            }
            weekStart = weekStart.minusWeeks(1)
        }
        return streak
    }
}
