package io.github.ddiling.mydayboard

import io.github.ddiling.mydayboard.core.testing.MutableClock
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

class HeaderClockStateTest {
    private val warsaw = ZoneId.of("Europe/Warsaw")
    private val london = ZoneId.of("Europe/London")

    private fun givenClockAt(
        instant: String,
        zone: ZoneId = ZoneOffset.UTC,
    ) = MutableClock(Instant.parse(instant), zone)

    @Test
    fun `time is formatted as HH-MM with two digits at 07-05`() {
        val state = HeaderClockState(givenClockAt("2025-01-03T07:05:00Z"))

        assertEquals("07:05", state.now(Locale.ENGLISH).time)
    }

    @Test
    fun `date is formatted DD-MM with leading zeros on 3 January`() {
        val state = HeaderClockState(givenClockAt("2025-01-03T07:05:00Z"))

        assertEquals("03.01", state.now(Locale.ENGLISH).date)
    }

    @Test
    fun `weekday is the short English name for Saturday`() {
        val state = HeaderClockState(givenClockAt("2025-01-04T10:00:00Z"))

        assertEquals("Sat", state.now(Locale.ENGLISH).weekday)
    }

    @Test
    fun `weekday is the short Polish name for Saturday under the Polish locale`() {
        val state = HeaderClockState(givenClockAt("2025-01-04T10:00:00Z"))

        assertEquals("sob.", state.now(Locale.forLanguageTag("pl-PL")).weekday)
    }

    @Test
    fun `a clock moved from 23-59 to 00-00 changes the date and the weekday`() {
        val clock = givenClockAt("2025-01-03T23:59:00Z")
        val state = HeaderClockState(clock)

        clock.advanceBy(Duration.ofMinutes(1))

        assertEquals(HeaderTime(time = "00:00", date = "04.01", weekday = "Sat"), state.now(Locale.ENGLISH))
    }

    @Test
    fun `a clock whose zone changes from Warsaw to London shows the new local time`() {
        val clock = givenClockAt("2025-01-03T12:00:00Z", warsaw)
        val state = HeaderClockState(clock)
        val timeInWarsaw = state.now(Locale.ENGLISH).time

        clock.zone = london

        assertEquals("13:00" to "12:00", timeInWarsaw to state.now(Locale.ENGLISH).time)
    }

    @Test
    fun `a DST jump in Warsaw from 02-00 to 03-00 shows 03-00 and not 02-30`() {
        val state = HeaderClockState(givenClockAt("2025-03-30T01:00:00Z", warsaw))

        assertEquals("03:00", state.now(Locale.ENGLISH).time)
    }

    @Test
    fun `millisToNextMinute is 60000 on the full minute`() {
        val state = HeaderClockState(givenClockAt("2025-01-03T07:05:00.000Z"))

        assertEquals(60_000L, state.millisToNextMinute())
    }

    @Test
    fun `millisToNextMinute is 1 one millisecond before the next minute`() {
        val state = HeaderClockState(givenClockAt("2025-01-03T07:05:59.999Z"))

        assertEquals(1L, state.millisToNextMinute())
    }
}
