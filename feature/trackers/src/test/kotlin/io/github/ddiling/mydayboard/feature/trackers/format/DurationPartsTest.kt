package io.github.ddiling.mydayboard.feature.trackers.format

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration

class DurationPartsTest {
    @Test
    fun `zero seconds is just now`() {
        assertEquals(DurationParts.JustNow, Duration.ZERO.toParts())
    }

    @Test
    fun `59 seconds is still just now`() {
        assertEquals(DurationParts.JustNow, Duration.ofSeconds(59).toParts())
    }

    @Test
    fun `a negative duration is just now`() {
        assertEquals(DurationParts.JustNow, Duration.ofMinutes(-5).toParts())
    }

    @Test
    fun `one minute is one minute`() {
        assertEquals(DurationParts.Minutes(1), Duration.ofMinutes(1).toParts())
    }

    @Test
    fun `59 minutes stays in minutes`() {
        assertEquals(DurationParts.Minutes(59), Duration.ofMinutes(59).toParts())
    }

    @Test
    fun `one hour is one hour and no minutes`() {
        assertEquals(DurationParts.Hours(1, 0), Duration.ofHours(1).toParts())
    }

    @Test
    fun `23 hours 59 minutes stays in hours`() {
        assertEquals(DurationParts.Hours(23, 59), Duration.ofMinutes(23 * 60 + 59).toParts())
    }

    @Test
    fun `one day is one day`() {
        assertEquals(DurationParts.Days(1), Duration.ofDays(1).toParts())
    }

    @Test
    fun `400 days is 400 days`() {
        assertEquals(DurationParts.Days(400), Duration.ofDays(400).toParts())
    }

    @Test
    fun `days drop the hours that do not fill another day`() {
        assertEquals(DurationParts.Days(2), Duration.ofHours(2 * 24 + 23).toParts())
    }

    @Test
    fun `a day count beyond Int saturates`() {
        assertEquals(Int.MAX_VALUE, (Int.MAX_VALUE.toLong() + 1).toIntSaturated())
    }

    @Test
    fun `a negative day count saturates to zero`() {
        assertEquals(0, (-1L).toIntSaturated())
    }
}
