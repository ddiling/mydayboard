package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** How many events happened on one local calendar [date]. */
data class DayCount(
    val date: LocalDate,
    val count: Int,
)

/**
 * Counts events per local calendar day for the [days] days ending with [today], for example the "last 7 days"
 * chart of an event counter.
 *
 * The result has exactly [days] entries, oldest first, and the last one is [today]. Days without events are
 * included with a count of 0, so a chart can draw one bar per entry without filling gaps itself.
 *
 * Each event belongs to the local date it falls on in [zone], so days with a daylight-saving change (23 or 25
 * hours long) still count every event of that day. Events outside the window are ignored. [eventTimes] may be in
 * any order.
 *
 * @throws IllegalArgumentException if [days] is not positive, because an empty window is always a caller bug.
 */
fun dailyCounts(
    eventTimes: List<Instant>,
    today: LocalDate,
    days: Int,
    zone: ZoneId,
): List<DayCount> {
    require(days > 0) { "days must be positive, was $days" }
    val countsByDate = eventTimes.groupingBy { it.atZone(zone).toLocalDate() }.eachCount()
    val firstDay = today.minusDays(days - 1L)
    return List(days) { offset ->
        val date = firstDay.plusDays(offset.toLong())
        DayCount(date = date, count = countsByDate[date] ?: 0)
    }
}
