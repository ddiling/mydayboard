package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private val WARSAW: ZoneId = ZoneId.of("Europe/Warsaw")

/** Parses a local date-time such as "2026-11-19T12:00" as Warsaw time. */
private fun warsaw(localDateTime: String): Instant = LocalDateTime.parse(localDateTime).atZone(WARSAW).toInstant()

class DailyCountsTest {
    private val today = LocalDate.parse("2026-11-19")

    @Test
    fun `there is exactly one entry per requested day`() {
        val counts = dailyCounts(emptyList(), today, days = 7, zone = WARSAW)

        assertEquals(7, counts.size)
    }

    @Test
    fun `days are listed oldest first and end with today`() {
        val counts = dailyCounts(emptyList(), today, days = 3, zone = WARSAW)

        assertEquals(listOf("2026-11-17", "2026-11-18", "2026-11-19").map(LocalDate::parse), counts.map { it.date })
    }

    @Test
    fun `days without events count zero`() {
        val events = listOf(warsaw("2026-11-17T09:00"), warsaw("2026-11-17T18:00"), warsaw("2026-11-19T07:00"))

        val counts = dailyCounts(events, today, days = 3, zone = WARSAW)

        assertEquals(listOf(2, 0, 1), counts.map { it.count })
    }

    @Test
    fun `input order does not matter`() {
        val events = listOf(warsaw("2026-11-19T07:00"), warsaw("2026-11-17T18:00"), warsaw("2026-11-17T09:00"))

        val counts = dailyCounts(events, today, days = 3, zone = WARSAW)

        assertEquals(listOf(2, 0, 1), counts.map { it.count })
    }

    @Test
    fun `events outside the window are ignored`() {
        val events = listOf(warsaw("2026-11-16T23:59:59.999"), warsaw("2026-11-17T00:00"), warsaw("2026-11-20T00:00"))

        val counts = dailyCounts(events, today, days = 3, zone = WARSAW)

        assertEquals(listOf(1, 0, 0), counts.map { it.count })
    }

    @Test
    fun `a 23-hour spring-forward day counts every event of its local day`() {
        val events = listOf("28T23:59", "29T00:00", "29T03:00", "29T23:59", "30T00:00").map { warsaw("2026-03-$it") }

        val counts = dailyCounts(events, LocalDate.parse("2026-03-30"), days = 3, zone = WARSAW)

        assertEquals(listOf(1, 3, 1), counts.map { it.count })
    }

    @Test
    fun `a 25-hour fall-back day counts every event of its local day`() {
        val secondHalfPastTwo = LocalDateTime.parse("2026-10-25T02:30").atZone(WARSAW).withLaterOffsetAtOverlap()
        val events =
            listOf("24T23:59", "25T00:00", "25T02:30", "25T23:59", "26T00:00").map { warsaw("2026-10-$it") } +
                secondHalfPastTwo.toInstant()

        val counts = dailyCounts(events, LocalDate.parse("2026-10-26"), days = 3, zone = WARSAW)

        assertEquals(listOf(1, 4, 1), counts.map { it.count })
    }

    @Test
    fun `zero days are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            dailyCounts(emptyList(), today, days = 0, zone = WARSAW)
        }
    }
}
