package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class SummarizeDaysSinceTest {
    private val warsawZone = ZoneId.of("Europe/Warsaw")

    @Test
    fun `without resets the current streak counts calendar days since the start`() {
        val startedAt = warsaw(2026, 11, 10, 12, 0)
        val now = warsaw(2026, 11, 19, 9, 0)

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(
            DaysSinceSummary(currentDays = 9, currentStart = startedAt, longestDays = 9, brokenStreaks = emptyList()),
            summary,
        )
    }

    @Test
    fun `a tracker started today shows zero days`() {
        val startedAt = warsaw(2026, 11, 19, 0, 0)
        val now = warsaw(2026, 11, 19, 23, 59)

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(0L, summary.currentDays)
    }

    @Test
    fun `a reset late in the evening counts as one day just after midnight`() {
        val reset = warsaw(2026, 11, 18, 23, 50)
        val now = warsaw(2026, 11, 19, 0, 10)

        val summary = summarizeDaysSince(warsaw(2026, 11, 1, 8, 0), listOf(reset), now, warsawZone)

        assertEquals(1L, summary.currentDays)
    }

    @Test
    fun `a reset exactly now starts a new streak at zero days`() {
        val now = warsaw(2026, 11, 19, 9, 0)

        val summary = summarizeDaysSince(warsaw(2026, 11, 1, 8, 0), listOf(now), now, warsawZone)

        assertEquals(0L, summary.currentDays)
        assertEquals(now, summary.currentStart)
    }

    @Test
    fun `two resets on the same day list a zero-day broken streak`() {
        val startedAt = warsaw(2026, 11, 1, 8, 0)
        val morningReset = warsaw(2026, 11, 15, 8, 0)
        val eveningReset = warsaw(2026, 11, 15, 20, 0)

        val summary =
            summarizeDaysSince(startedAt, listOf(morningReset, eveningReset), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(
            listOf(
                Streak(start = morningReset, end = eveningReset, days = 0),
                Streak(start = startedAt, end = morningReset, days = 14),
            ),
            summary.brokenStreaks,
        )
    }

    @Test
    fun `the longest streak is the current one when it outlasts every broken streak`() {
        val startedAt = warsaw(2026, 11, 1, 8, 0)
        val reset = warsaw(2026, 11, 4, 8, 0)

        val summary = summarizeDaysSince(startedAt, listOf(reset), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(15L, summary.longestDays)
    }

    @Test
    fun `the longest streak is a broken one when it outlasts the current streak`() {
        val startedAt = warsaw(2026, 11, 1, 8, 0)
        val firstReset = warsaw(2026, 11, 11, 8, 0)
        val secondReset = warsaw(2026, 11, 14, 8, 0)

        val summary =
            summarizeDaysSince(startedAt, listOf(firstReset, secondReset), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(10L, summary.longestDays)
    }

    @Test
    fun `when the current and a broken streak are equally long that length is the longest`() {
        val startedAt = warsaw(2026, 11, 9, 8, 0)
        val reset = warsaw(2026, 11, 14, 8, 0)

        val summary = summarizeDaysSince(startedAt, listOf(reset), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(5L, summary.longestDays)
    }

    @Test
    fun `resets given out of order are sorted into streaks listed newest first`() {
        val startedAt = warsaw(2026, 11, 1, 8, 0)
        val first = warsaw(2026, 11, 5, 8, 0)
        val second = warsaw(2026, 11, 12, 8, 0)
        val third = warsaw(2026, 11, 15, 8, 0)

        val summary =
            summarizeDaysSince(startedAt, listOf(second, third, first), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(
            DaysSinceSummary(
                currentDays = 4,
                currentStart = third,
                longestDays = 7,
                brokenStreaks =
                    listOf(
                        Streak(start = second, end = third, days = 3),
                        Streak(start = first, end = second, days = 7),
                        Streak(start = startedAt, end = first, days = 4),
                    ),
            ),
            summary,
        )
    }

    @Test
    fun `resets before the tracker started are ignored`() {
        val startedAt = warsaw(2026, 11, 10, 8, 0)
        val now = warsaw(2026, 11, 19, 9, 0)

        val summary = summarizeDaysSince(startedAt, listOf(warsaw(2026, 11, 10, 7, 59)), now, warsawZone)

        assertEquals(
            DaysSinceSummary(currentDays = 9, currentStart = startedAt, longestDays = 9, brokenStreaks = emptyList()),
            summary,
        )
    }

    @Test
    fun `resets after now are ignored`() {
        val startedAt = warsaw(2026, 11, 10, 8, 0)
        val now = warsaw(2026, 11, 19, 9, 0)

        val summary = summarizeDaysSince(startedAt, listOf(warsaw(2026, 11, 19, 9, 1)), now, warsawZone)

        assertEquals(
            DaysSinceSummary(currentDays = 9, currentStart = startedAt, longestDays = 9, brokenStreaks = emptyList()),
            summary,
        )
    }

    @Test
    fun `a start after now shows zero days instead of a negative count`() {
        val startedAt = warsaw(2026, 11, 20, 8, 0)

        val summary = summarizeDaysSince(startedAt, emptyList(), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(0L, summary.currentDays)
    }

    @Test
    fun `the 23-hour spring-forward day still counts as one day`() {
        // On 2026-03-29 Warsaw skips 02:00-03:00, so this is only 23 hours of real time.
        val startedAt = warsaw(2026, 3, 29, 0, 0)
        val now = warsaw(2026, 3, 30, 0, 0)

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(1L, summary.currentDays)
    }

    @Test
    fun `the 25-hour fall-back day counts as zero days until midnight`() {
        // On 2026-10-25 Warsaw repeats 02:00-03:00, so this is 24.5 hours of real time on one calendar day.
        val startedAt = warsaw(2026, 10, 25, 0, 0)
        val now = warsaw(2026, 10, 25, 23, 30)

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(0L, summary.currentDays)
    }

    @Test
    fun `a broken streak across the fall-back night counts calendar days`() {
        val startedAt = warsaw(2026, 10, 24, 23, 30)
        val reset = warsaw(2026, 10, 26, 0, 10)

        val summary = summarizeDaysSince(startedAt, listOf(reset), warsaw(2026, 11, 19, 9, 0), warsawZone)

        assertEquals(listOf(Streak(start = startedAt, end = reset, days = 2)), summary.brokenStreaks)
    }

    @Test
    fun `the leap day 2028-02-29 is counted`() {
        val startedAt = warsaw(2028, 2, 28, 12, 0)
        val now = warsaw(2028, 3, 1, 12, 0)

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(2L, summary.currentDays)
    }

    @Test
    fun `the same instants on one Warsaw day count zero days`() {
        // 15:30 and 16:30 in Warsaw (UTC+1): the same calendar day.
        val startedAt = Instant.parse("2026-11-18T14:30:00Z")
        val now = Instant.parse("2026-11-18T15:30:00Z")

        val summary = summarizeDaysSince(startedAt, emptyList(), now, warsawZone)

        assertEquals(0L, summary.currentDays)
    }

    @Test
    fun `the same instants across a Tokyo midnight count one day`() {
        // 23:30 and 00:30 the next day in Tokyo (UTC+9): a midnight lies in between.
        val startedAt = Instant.parse("2026-11-18T14:30:00Z")
        val now = Instant.parse("2026-11-18T15:30:00Z")

        val summary = summarizeDaysSince(startedAt, emptyList(), now, ZoneId.of("Asia/Tokyo"))

        assertEquals(1L, summary.currentDays)
    }

    private fun warsaw(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, warsawZone).toInstant()
}
