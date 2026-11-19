package io.github.ddiling.mydayboard.core.testing

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalTime

class MutableClockTest {
    private val clock = MutableClock(warsaw(2026, 3, 2, 10), TestZones.WARSAW)

    @Test
    fun `advanceBy moves the instant forward`() {
        clock.advanceBy(Duration.ofMinutes(90))

        assertEquals(warsaw(2026, 3, 2, 11, 30), clock.instant())
    }

    @Test
    fun `changing the zone keeps the instant and changes the local time`() {
        clock.zone = TestZones.TOKYO

        assertEquals(warsaw(2026, 3, 2, 10), clock.instant())
        assertEquals(LocalTime.of(18, 0), LocalTime.now(clock))
    }

    @Test
    fun `a copy in another zone keeps following the original's instant`() {
        val utcClock = clock.withZone(TestZones.UTC)

        clock.advanceBy(Duration.ofHours(1))

        assertEquals(TestZones.UTC, utcClock.zone)
        assertEquals(warsaw(2026, 3, 2, 11), utcClock.instant())
    }
}
