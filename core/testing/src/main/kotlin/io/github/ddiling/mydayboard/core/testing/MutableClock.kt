package io.github.ddiling.mydayboard.core.testing

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * A [Clock] that a test sets and moves by hand. Unlike [Clock.fixed], it can move while the code under test
 * keeps the same instance (as production code keeps one `LiveZoneClock`): [advanceBy] lets time pass, and
 * setting [zone] simulates the user travelling.
 */
class MutableClock(
    var now: Instant,
    // Clock already has a Java method getZone(), which the property's getter would clash with on the JVM,
    // so the getter gets another JVM name. Kotlin code still reads and writes it as `clock.zone`.
    @get:JvmName("currentZone")
    var zone: ZoneId,
) : Clock() {
    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    /** A copy in [zone] that keeps reading this clock's [now], as `Clock.system(zone)` keeps following real time. */
    override fun withZone(zone: ZoneId): Clock = ZonedView(zone)

    /** Moves [now] forward by [duration] (backward if it is negative). */
    fun advanceBy(duration: Duration) {
        now += duration
    }

    private inner class ZonedView(
        private val viewZone: ZoneId,
    ) : Clock() {
        override fun instant(): Instant = now

        override fun getZone(): ZoneId = viewZone

        override fun withZone(zone: ZoneId): Clock = ZonedView(zone)
    }
}
