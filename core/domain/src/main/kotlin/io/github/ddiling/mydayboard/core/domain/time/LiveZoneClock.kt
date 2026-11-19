package io.github.ddiling.mydayboard.core.domain.time

import java.time.Clock
import java.time.Instant
import java.time.ZoneId

/**
 * The production [Clock]: the real current instant, in the device's time zone *as it is right now*.
 * The only class allowed to read the real time; everything else takes a [Clock], so tests can control it.
 *
 * Why not [Clock.systemDefaultZone]: it reads [ZoneId.systemDefault] once, when the clock is created. The app
 * keeps one clock for its whole life, so after the user travels or changes the zone, "today" would stay in the
 * old zone until a restart. This clock reads the default zone on every [getZone] call instead (Android updates
 * the process's default zone when the device zone changes).
 */
class LiveZoneClock : Clock() {
    override fun instant(): Instant = Instant.now()

    override fun getZone(): ZoneId = ZoneId.systemDefault()

    /** A clock pinned to [zone] on purpose, e.g. to compute in UTC. Only this one stops following the device. */
    override fun withZone(zone: ZoneId): Clock = Clock.system(zone)
}
