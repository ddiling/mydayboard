package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Direction of an event counter over the last week compared with the week before. */
enum class Trend {
    /** More events in the last 7 complete days than in the 7 days before. */
    UP,

    /** Fewer events in the last 7 complete days than in the 7 days before. */
    DOWN,

    /** The same number of events in both weeks. */
    FLAT,

    /** The tracker's history does not cover both weeks, so a comparison would be misleading. */
    NOT_ENOUGH_DATA,
}

/** Length of each compared window, in days. */
private const val TREND_WINDOW_DAYS = 7L

/**
 * Compares the last two complete weeks of an event counter (decision D13).
 *
 * - Recent week: today−7 … today−1. Previous week: today−14 … today−8. Both are local calendar days in [zone],
 *   from 00:00, so a week containing a daylight-saving change still covers exactly its seven local days.
 * - Today is left out because it is not over yet: comparing a half day with full days would show DOWN every
 *   morning.
 * - The sums are compared directly: greater is [Trend.UP], smaller is [Trend.DOWN], equal is [Trend.FLAT]. There is
 *   no percentage threshold, so the result is easy to explain and to test.
 *
 * [historyStart] is when the tracker's history begins: the earlier of when tracking started and the tracker's
 * first event. The rule: if [historyStart] is after 00:00 of today−14 in [zone], the result is
 * [Trend.NOT_ENOUGH_DATA]. A history starting at 00:00 fourteen days ago is enough; one starting later that day,
 * or thirteen days ago, is not. The reason: time before [historyStart] counts as zero events only because nothing
 * was being tracked, so a partly covered first day would undercount the previous week and bias the result
 * towards [Trend.UP]. The cost is at most one extra day without a trend.
 *
 * [eventTimes] are the tracker's event times, in any order. Events outside the two weeks, including today's, are
 * ignored.
 */
fun counterTrend(
    eventTimes: List<Instant>,
    historyStart: Instant,
    today: LocalDate,
    zone: ZoneId,
): Trend {
    val previousWeekStart = today.minusDays(2 * TREND_WINDOW_DAYS).atStartOfDay(zone).toInstant()
    val recentWeekStart = today.minusDays(TREND_WINDOW_DAYS).atStartOfDay(zone).toInstant()
    val todayStart = today.atStartOfDay(zone).toInstant()
    if (historyStart.isAfter(previousWeekStart)) return Trend.NOT_ENOUGH_DATA

    // Each week is a half-open range [start, end): an event at exactly 00:00 belongs to the day that starts then.
    val previous = eventTimes.count { !it.isBefore(previousWeekStart) && it.isBefore(recentWeekStart) }
    val recent = eventTimes.count { !it.isBefore(recentWeekStart) && it.isBefore(todayStart) }
    return when {
        recent > previous -> Trend.UP
        recent < previous -> Trend.DOWN
        else -> Trend.FLAT
    }
}
