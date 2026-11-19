package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

/** Parses a local date-time such as "2026-11-19T12:00" as Warsaw time. */
private fun warsaw(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(WARSAW).toInstant()

/** Today is 2026-11-19. Recent week: 2026-11-12 … 2026-11-18. Previous week: 2026-11-05 … 2026-11-11. */
class CounterTrendTest {
    private val today = LocalDate.parse("2026-11-19")
    private val longHistory = warsaw("2026-01-01T00:00")

    private fun trendOf(
        vararg localDateTimes: String,
        historyStart: Instant = longHistory,
    ): Trend = counterTrend(localDateTimes.map(::warsaw), historyStart, today, WARSAW)

    @Test
    fun `more events in the recent week is up`() {
        val trend = trendOf("2026-11-08T09:00", "2026-11-13T09:00", "2026-11-15T09:00")

        assertEquals(Trend.UP, trend)
    }

    @Test
    fun `fewer events in the recent week is down`() {
        val trend = trendOf("2026-11-06T09:00", "2026-11-10T09:00", "2026-11-15T09:00")

        assertEquals(Trend.DOWN, trend)
    }

    @Test
    fun `the same number of events in both weeks is flat`() {
        val trend = trendOf("2026-11-06T09:00", "2026-11-13T09:00")

        assertEquals(Trend.FLAT, trend)
    }

    @Test
    fun `events today do not affect the trend`() {
        val trend = trendOf("2026-11-06T09:00", "2026-11-13T09:00", "2026-11-19T00:00", "2026-11-19T08:00")

        assertEquals(Trend.FLAT, trend)
    }

    @Test
    fun `events before the previous week do not affect the trend`() {
        val trend = trendOf("2026-11-04T23:59:59.999", "2026-11-13T09:00")

        assertEquals(Trend.UP, trend)
    }

    @Test
    fun `history starting 13 days ago is not enough data`() {
        val trend = trendOf("2026-11-13T09:00", historyStart = warsaw("2026-11-06T00:00"))

        assertEquals(Trend.NOT_ENOUGH_DATA, trend)
    }

    @Test
    fun `history starting at midnight 14 days ago is enough data`() {
        val trend = trendOf("2026-11-13T09:00", historyStart = warsaw("2026-11-05T00:00"))

        assertEquals(Trend.UP, trend)
    }

    @Test
    fun `history starting later on the day 14 days ago is not enough data because that day is not fully covered`() {
        val trend = trendOf("2026-11-13T09:00", historyStart = warsaw("2026-11-05T00:00:01"))

        assertEquals(Trend.NOT_ENOUGH_DATA, trend)
    }

    @Test
    fun `an event at the start of today minus 7 belongs to the recent week`() {
        val trend = trendOf("2026-11-12T00:00")

        assertEquals(Trend.UP, trend)
    }

    @Test
    fun `an event at the end of today minus 8 belongs to the previous week`() {
        val trend = trendOf("2026-11-11T23:59:59.999")

        assertEquals(Trend.DOWN, trend)
    }

    @Test
    fun `an event at the start of today minus 14 belongs to the previous week`() {
        val trend = trendOf("2026-11-05T00:00")

        assertEquals(Trend.DOWN, trend)
    }

    @Test
    fun `weeks follow local midnight across a spring-forward change`() {
        // The recent week (2026-03-29 … 2026-04-04) is only 167 hours long, so "7 x 24 hours before today" would be
        // 2026-03-28 23:00 and would wrongly put this event into the recent week.
        val events = listOf(warsaw("2026-03-28T23:30"))

        val trend = counterTrend(events, longHistory, LocalDate.parse("2026-04-05"), WARSAW)

        assertEquals(Trend.DOWN, trend)
    }

    @Test
    fun `history starting at midnight 14 days ago is enough data across a spring-forward change`() {
        // The previous week (2026-03-29 … 2026-04-04) is only 167 hours long, so "14 x 24 hours before today" would be
        // 2026-03-28 23:00 and would wrongly treat this history as starting too late.
        val events = listOf(warsaw("2026-04-06T09:00"))

        val trend = counterTrend(events, warsaw("2026-03-29T00:00"), LocalDate.parse("2026-04-12"), WARSAW)

        assertEquals(Trend.UP, trend)
    }
}
