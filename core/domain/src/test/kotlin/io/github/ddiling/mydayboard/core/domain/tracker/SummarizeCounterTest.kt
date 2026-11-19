package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

/** Parses a local date-time such as "2026-11-19T12:00" as Warsaw time. */
private fun warsaw(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(WARSAW).toInstant()

class SummarizeCounterTest {
    private val now = warsaw("2026-11-19T12:00")

    @Test
    fun `no events give zero today and no last event`() {
        val summary = summarizeCounter(emptyList(), now, WARSAW)

        assertEquals(CounterSummary(todayCount = 0, lastEventAt = null, sinceLast = null), summary)
    }

    @Test
    fun `an event one millisecond before midnight is not counted today`() {
        val summary = summarizeCounter(listOf(warsaw("2026-11-18T23:59:59.999")), now, WARSAW)

        assertEquals(0, summary.todayCount)
    }

    @Test
    fun `an event at midnight is counted today`() {
        val summary = summarizeCounter(listOf(warsaw("2026-11-19T00:00")), now, WARSAW)

        assertEquals(1, summary.todayCount)
    }

    @Test
    fun `today is the day of now in the given zone, not in UTC`() {
        // 00:30 in Warsaw is still 2026-11-18 in UTC.
        val justAfterMidnight = warsaw("2026-11-19T00:30")

        val summary = summarizeCounter(listOf(warsaw("2026-11-19T00:10")), justAfterMidnight, WARSAW)

        assertEquals(1, summary.todayCount)
    }

    @Test
    fun `an event exactly at now counts`() {
        val summary = summarizeCounter(listOf(now), now, WARSAW)

        assertEquals(CounterSummary(todayCount = 1, lastEventAt = now, sinceLast = Duration.ZERO), summary)
    }

    @Test
    fun `the last event is the latest one whatever the input order`() {
        val events = listOf(warsaw("2026-11-19T08:00"), warsaw("2026-11-19T09:00"), warsaw("2026-11-17T20:00"))

        val summary = summarizeCounter(events, now, WARSAW)

        assertEquals(warsaw("2026-11-19T09:00"), summary.lastEventAt)
    }

    @Test
    fun `an event in the future is ignored`() {
        val events = listOf(warsaw("2026-11-19T09:00"), warsaw("2026-11-19T15:00"))

        val summary = summarizeCounter(events, now, WARSAW)

        val expected = CounterSummary(1, lastEventAt = warsaw("2026-11-19T09:00"), sinceLast = Duration.ofHours(3))
        assertEquals(expected, summary)
    }

    @Test
    fun `time since the last event is exact`() {
        val summary = summarizeCounter(listOf(warsaw("2026-11-18T09:56:55.995")), now, WARSAW)

        assertEquals(Duration.parse("PT26H3M4.005S"), summary.sinceLast)
    }

    @Test
    fun `the same instants give a different count today in another time zone`() {
        // 2026-11-18 20:00 in Warsaw is already 2026-11-19 04:00 in Tokyo.
        val events = listOf(warsaw("2026-11-18T20:00"), warsaw("2026-11-19T06:00"))
        val nowAt = warsaw("2026-11-19T07:00")

        val inWarsaw = summarizeCounter(events, nowAt, WARSAW).todayCount
        val inTokyo = summarizeCounter(events, nowAt, ZoneId.of("Asia/Tokyo")).todayCount

        assertEquals(1, inWarsaw)
        assertEquals(2, inTokyo)
    }

    @Test
    fun `all events of a 23-hour spring-forward day are counted`() {
        // On 2026-03-29 Warsaw clocks jump from 02:00 to 03:00.
        val events = listOf("28T23:59", "29T00:00", "29T01:59", "29T03:00", "29T23:59").map { warsaw("2026-03-$it") }

        val summary = summarizeCounter(events, warsaw("2026-03-29T23:59:59"), WARSAW)

        assertEquals(4, summary.todayCount)
    }

    @Test
    fun `all events of a 25-hour fall-back day are counted`() {
        // On 2026-10-25 Warsaw clocks go back from 03:00 to 02:00, so 02:30 happens twice.
        val secondHalfPastTwo = LocalDateTime.parse("2026-10-25T02:30").atZone(WARSAW).withLaterOffsetAtOverlap()
        val events =
            listOf("24T23:59", "25T00:00", "25T02:30", "25T23:59").map { warsaw("2026-10-$it") } +
                secondHalfPastTwo.toInstant()

        val summary = summarizeCounter(events, warsaw("2026-10-25T23:59:59"), WARSAW)

        assertEquals(4, summary.todayCount)
    }
}
