package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * One uninterrupted run of a "days since" tracker, from a start (the tracker's start or a reset)
 * until the next reset.
 *
 * @property start when the streak began, in UTC.
 * @property end the reset that broke the streak, or `null` for the current streak, which is still running.
 * @property days whole calendar days the streak covered, counted as in [summarizeDaysSince].
 */
data class Streak(
    val start: Instant,
    val end: Instant?,
    val days: Long,
)

/**
 * Everything the UI shows for a "days since" tracker, already computed, so the screen only formats it.
 *
 * @property currentDays calendar days since the last reset (or since the tracker started, if never reset).
 * @property currentStart when the current streak began: the last reset, or the tracker's start.
 * @property longestDays the longest streak so far, the current one included, so a running record shows as one.
 * @property brokenStreaks the streaks ended by a reset, newest first, because the UI lists the most recent at the top.
 */
data class DaysSinceSummary(
    val currentDays: Long,
    val currentStart: Instant,
    val longestDays: Long,
    val brokenStreaks: List<Streak>,
)

/**
 * Summarizes a "days since" tracker started at [startedAt] and reset at each of [eventTimes].
 *
 * The tracker's start and the resets, in time order, are the boundaries of the streaks: each pair of
 * neighbouring boundaries is a broken streak, and the last boundary up to [now] is the current one.
 *
 * Days are **calendar days** in [zone], not 24-hour periods: a reset at 23:50 shows 1 day at 00:10 the
 * next morning. That is how people count ("3 days without"), and it stays correct on daylight-saving
 * days, which last 23 or 25 hours.
 *
 * [now] and [zone] are parameters, never read from the system, so the result is deterministic and testable.
 *
 * Reset times before [startedAt] or after [now] are ignored. Validation ([validateEventTime]) keeps
 * them out, but a summary must not break on bad data, for example after the device clock was changed.
 * For the same reason a [startedAt] after [now] shows 0 days rather than a negative count.
 *
 * @param eventTimes the reset times in any order; they are sorted here.
 */
fun summarizeDaysSince(
    startedAt: Instant,
    eventTimes: List<Instant>,
    now: Instant,
    zone: ZoneId,
): DaysSinceSummary {
    val resets = eventTimes.filter { it in startedAt..now }.sorted()
    val boundaries = listOf(startedAt) + resets

    val brokenStreaksOldestFirst =
        boundaries.zipWithNext { start, end ->
            Streak(start = start, end = end, days = calendarDaysBetween(start, end, zone))
        }
    val currentStart = boundaries.last()
    val currentDays = calendarDaysBetween(currentStart, now, zone).coerceAtLeast(0)
    val longestBrokenDays = brokenStreaksOldestFirst.maxOfOrNull { it.days } ?: 0

    return DaysSinceSummary(
        currentDays = currentDays,
        currentStart = currentStart,
        longestDays = maxOf(currentDays, longestBrokenDays),
        brokenStreaks = brokenStreaksOldestFirst.reversed(),
    )
}

/**
 * Counts the midnights crossed between [from] and [to] in [zone]. Comparing local dates (instead of
 * dividing a duration by 24 hours) is what makes the count match the calendar on daylight-saving days.
 */
private fun calendarDaysBetween(
    from: Instant,
    to: Instant,
    zone: ZoneId,
): Long = ChronoUnit.DAYS.between(from.atZone(zone).toLocalDate(), to.atZone(zone).toLocalDate())
