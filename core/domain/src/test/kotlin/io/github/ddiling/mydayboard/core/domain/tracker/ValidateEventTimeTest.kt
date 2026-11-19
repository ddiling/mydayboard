package io.github.ddiling.mydayboard.core.domain.tracker

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class ValidateEventTimeTest {
    private val now = warsaw(2026, 11, 19, 9, 0)
    private val trackerStart = warsaw(2026, 11, 1, 8, 0)

    @Test
    fun `a time equal to now is valid`() {
        val result = validateEventTime(occurredAt = now, earliestAllowed = trackerStart, now = now)

        assertEquals(EventTimeValidation.Valid, result)
    }

    @Test
    fun `a time one millisecond after now is in the future`() {
        val result = validateEventTime(occurredAt = now.plusMillis(1), earliestAllowed = trackerStart, now = now)

        assertEquals(EventTimeValidation.InFuture, result)
    }

    @Test
    fun `a time before the earliest allowed is before the tracker start`() {
        val result =
            validateEventTime(occurredAt = trackerStart.minusMillis(1), earliestAllowed = trackerStart, now = now)

        assertEquals(EventTimeValidation.BeforeTrackerStart, result)
    }

    @Test
    fun `a time equal to the earliest allowed is valid`() {
        val result = validateEventTime(occurredAt = trackerStart, earliestAllowed = trackerStart, now = now)

        assertEquals(EventTimeValidation.Valid, result)
    }

    @Test
    fun `without a lower bound any past time is valid`() {
        val longAgo = warsaw(2000, 1, 1, 0, 0)

        val result = validateEventTime(occurredAt = longAgo, earliestAllowed = null, now = now)

        assertEquals(EventTimeValidation.Valid, result)
    }

    @Test
    fun `a time in the future and before a future bound is reported as in the future`() {
        val futureBound = now.plusSeconds(3_600)
        val occurredAt = now.plusSeconds(60)

        val result = validateEventTime(occurredAt = occurredAt, earliestAllowed = futureBound, now = now)

        assertEquals(EventTimeValidation.InFuture, result)
    }

    private fun warsaw(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
    ): Instant = ZonedDateTime.of(year, month, day, hour, minute, 0, 0, ZoneId.of("Europe/Warsaw")).toInstant()
}
