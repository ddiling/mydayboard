package io.github.ddiling.mydayboard.feature.trackers.event

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.TimeZone

class EntryPickersTest {
    private val originalZone: TimeZone = TimeZone.getDefault()
    private val springChangeDay = LocalDate.of(2026, 3, 29)
    private val utcMidnightOfThatDay = Instant.parse("2026-03-29T00:00:00Z").toEpochMilli()
    private val farEast = TimeZone.getTimeZone("Pacific/Kiritimati")
    private val farWest = TimeZone.getTimeZone("Pacific/Pago_Pago")

    @After
    fun restoreDefaultZone() {
        TimeZone.setDefault(originalZone)
    }

    @Test
    fun `a date becomes the UTC midnight the picker expects in a zone far east of UTC`() {
        TimeZone.setDefault(farEast)

        assertEquals(utcMidnightOfThatDay, springChangeDay.toPickerMillis())
    }

    @Test
    fun `a date becomes the UTC midnight the picker expects in a zone far west of UTC`() {
        TimeZone.setDefault(farWest)

        assertEquals(utcMidnightOfThatDay, springChangeDay.toPickerMillis())
    }

    @Test
    fun `a picked UTC midnight is the same calendar date in a zone far east of UTC`() {
        TimeZone.setDefault(farEast)

        assertEquals(springChangeDay, utcMidnightOfThatDay.fromPickerMillis())
    }

    @Test
    fun `a picked UTC midnight is the same calendar date in a zone far west of UTC`() {
        TimeZone.setDefault(farWest)

        assertEquals(springChangeDay, utcMidnightOfThatDay.fromPickerMillis())
    }
}
