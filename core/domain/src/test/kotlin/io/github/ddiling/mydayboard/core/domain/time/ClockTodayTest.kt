package io.github.ddiling.mydayboard.core.domain.time

import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ClockTodayTest {
    @Test
    fun `today is still the old date at 23 59 59 999 and the new date at midnight`() {
        val lastMomentOfDay = warsaw(2026, 5, 10, 23, 59).plusSeconds(59).plusMillis(999)
        val clock = MutableClock(lastMomentOfDay, TestZones.WARSAW)

        val beforeMidnight = clock.today()
        clock.now = warsaw(2026, 5, 11)
        val atMidnight = clock.today()

        assertEquals(LocalDate.of(2026, 5, 10), beforeMidnight)
        assertEquals(LocalDate.of(2026, 5, 11), atMidnight)
    }

    @Test
    fun `today is correct at 00 30 on the spring forward day`() {
        val clock = MutableClock(warsaw(2026, 3, 29, 0, 30), TestZones.WARSAW)

        assertEquals(LocalDate.of(2026, 3, 29), clock.today())
    }

    @Test
    fun `today is correct at 00 30 on the fall back day`() {
        val clock = MutableClock(warsaw(2026, 10, 25, 0, 30), TestZones.WARSAW)

        assertEquals(LocalDate.of(2026, 10, 25), clock.today())
    }

    @Test
    fun `today follows a zone change with the same instant`() {
        val instant = warsaw(2026, 5, 10, 23, 30)
        val clock = MutableClock(instant, TestZones.WARSAW)

        val inWarsaw = clock.today()
        clock.zone = TestZones.TOKYO
        val inTokyo = clock.today()

        assertEquals(LocalDate.of(2026, 5, 10), inWarsaw)
        assertEquals(LocalDate.of(2026, 5, 11), inTokyo)
    }
}
