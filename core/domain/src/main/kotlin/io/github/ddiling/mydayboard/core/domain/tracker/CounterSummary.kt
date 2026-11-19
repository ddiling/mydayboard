package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Duration
import java.time.Instant
import java.time.ZoneId

/**
 * What an event counter shows at a glance. The UI decides how to phrase it (plurals, "never", relative time).
 *
 * @property todayCount Events during today's local calendar day.
 * @property lastEventAt The most recent event, or null if there is none yet.
 * @property sinceLast Time from [lastEventAt] to now, or null if there is no event yet. Exact, not rounded, so
 *   the UI can choose its own precision.
 */
data class CounterSummary(
    val todayCount: Int,
    val lastEventAt: Instant?,
    val sinceLast: Duration?,
)

/**
 * Summarizes an event counter as of [now].
 *
 * "Today" is the local calendar day in [zone] that contains [now], from 00:00 up to the next 00:00, because that
 * is what the user means by today, wherever they are. It is computed by converting each instant to a local date
 * rather than by subtracting 24 hours, so days with a daylight-saving change (23 or 25 hours long) still count
 * every event of that day.
 *
 * Events after [now] are ignored. The app never creates them, but a clock that was wrong when an event was
 * recorded, or an event synced from another device, could produce one, and a negative "time since last" would
 * make no sense on screen. An event exactly at [now] counts.
 *
 * [eventTimes] are the tracker's event times, in any order. Taking plain instants rather than event objects keeps
 * this function independent of how events are stored and easy to test.
 */
fun summarizeCounter(
    eventTimes: List<Instant>,
    now: Instant,
    zone: ZoneId,
): CounterSummary {
    val pastEvents = eventTimes.filterNot { it.isAfter(now) }
    val today = now.atZone(zone).toLocalDate()
    val todayCount = pastEvents.count { it.atZone(zone).toLocalDate() == today }
    val lastEventAt = pastEvents.maxOrNull()
    val sinceLast = lastEventAt?.let { Duration.between(it, now) }
    return CounterSummary(todayCount = todayCount, lastEventAt = lastEventAt, sinceLast = sinceLast)
}
