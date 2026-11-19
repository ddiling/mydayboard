package io.github.ddiling.mydayboard.core.domain.time

import io.github.ddiling.mydayboard.core.testing.TestZones
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.time.Instant
import java.util.TimeZone

// Reads the real time and changes the JVM default zone on purpose: that is what this clock wraps.
class LiveZoneClockTest {
    private val originalDefaultZone: TimeZone = TimeZone.getDefault()

    @After
    fun restoreDefaultZone() {
        TimeZone.setDefault(originalDefaultZone)
    }

    @Test
    fun `zone follows a change of the default time zone after the clock was created`() {
        TimeZone.setDefault(TimeZone.getTimeZone(TestZones.WARSAW))
        val clock = LiveZoneClock()

        TimeZone.setDefault(TimeZone.getTimeZone(TestZones.TOKYO))

        assertEquals(TestZones.TOKYO, clock.zone)
    }

    @Test
    fun `instant is the real current time`() {
        val clock = LiveZoneClock()

        val before = Instant.now()
        val instant = clock.instant()
        val after = Instant.now()

        assertFalse("$instant is before $before", instant.isBefore(before))
        assertFalse("$instant is after $after", instant.isAfter(after))
    }

    @Test
    fun `withZone returns a clock in that zone`() {
        TimeZone.setDefault(TimeZone.getTimeZone(TestZones.WARSAW))

        val clock = LiveZoneClock().withZone(TestZones.TOKYO)

        assertEquals(TestZones.TOKYO, clock.zone)
    }
}
