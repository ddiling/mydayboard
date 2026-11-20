package io.github.ddiling.mydayboard.feature.trackers.event

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate

class EntryPickersTest {
    private val springChangeDay = LocalDate.of(2026, 3, 29)
    private val utcMidnightOfThatDay = Instant.parse("2026-03-29T00:00:00Z").toEpochMilli()

    @Test
    fun `a date becomes the UTC midnight the picker expects`() {
        assertEquals(utcMidnightOfThatDay, springChangeDay.toPickerMillis())
    }

    @Test
    fun `a picked UTC midnight is the same calendar date whatever the device zone`() {
        assertEquals(springChangeDay, utcMidnightOfThatDay.fromPickerMillis())
    }
}
