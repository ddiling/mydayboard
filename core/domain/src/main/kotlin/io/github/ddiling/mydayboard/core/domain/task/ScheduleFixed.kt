package io.github.ddiling.mydayboard.core.domain.task

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/**
 * The windows of [task] in order, as a lazy sequence (it may be endless).
 *
 * Expiring tasks last only their due day. Staying tasks last until the day before the next due day, so a
 * missed day can be caught up but never overlaps the next one.
 */
internal fun dueWindows(
    task: Task,
    countedDays: List<LocalDate>,
): Sequence<DueWindow> {
    val repeat = task.repeat
    if (repeat is Repeat.EveryNDays && repeat.mode == EveryNDaysMode.FROM_LAST_DONE) {
        return fromLastWindows(task, repeat, countedDays)
    }
    val first = firstFixedDue(task.startDate, repeat)
    return generateSequence(first) { nextFixedDue(it, repeat) }.map { due ->
        val next = nextFixedDue(due, repeat)
        val lastDay = if (task.onMiss == OnMiss.EXPIRES) due else next?.minusDays(1)
        DueWindow(due, lastDay)
    }
}

private fun firstFixedDue(
    start: LocalDate,
    repeat: Repeat,
): LocalDate? =
    when (repeat) {
        is Repeat.TimesPerDay, Repeat.EveryDay -> {
            start
        }

        is Repeat.Weekdays -> {
            if (start.dayOfWeek in repeat.days) start else nextFixedDue(start, repeat)
        }

        is Repeat.EveryNDays -> {
            val behind = ChronoUnit.DAYS.between(repeat.firstDue, start)
            val skipped = if (behind > 0) (behind + repeat.days - 1) / repeat.days else 0
            repeat.firstDue.plusDays(skipped * repeat.days)
        }

        is Repeat.Monthly -> {
            val month = YearMonth.from(start)
            val thisMonth = clampedDay(month, repeat.dayOfMonth)
            if (thisMonth.isBefore(start)) clampedDay(month.plusMonths(1), repeat.dayOfMonth) else thisMonth
        }

        is Repeat.OneTime -> {
            repeat.due
        }
    }

private fun nextFixedDue(
    after: LocalDate,
    repeat: Repeat,
): LocalDate? =
    when (repeat) {
        is Repeat.TimesPerDay, Repeat.EveryDay -> after.plusDays(1)
        is Repeat.Weekdays -> nextWeekday(after, repeat.days)
        is Repeat.EveryNDays -> after.plusDays(repeat.days.toLong())
        is Repeat.Monthly -> clampedDay(YearMonth.from(after).plusMonths(1), repeat.dayOfMonth)
        is Repeat.OneTime -> null
    }

private fun nextWeekday(
    after: LocalDate,
    days: Set<DayOfWeek>,
): LocalDate? = (1L..DAYS_IN_WEEK).map { after.plusDays(it) }.firstOrNull { it.dayOfWeek in days }

/** The day of month, or the last day of a shorter month (31 in a 30-day month gives the 30th). */
private fun clampedDay(
    month: YearMonth,
    dayOfMonth: Int,
): LocalDate = month.atDay(minOf(dayOfMonth, month.lengthOfMonth()))

private const val DAYS_IN_WEEK = 7L
