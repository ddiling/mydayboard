package io.github.ddiling.mydayboard.feature.trackers.event

import io.github.ddiling.mydayboard.core.domain.tracker.EventTimeValidation
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.earliestAllowedEventTime
import io.github.ddiling.mydayboard.core.domain.tracker.validateEventTime
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit

/**
 * The instant the picked local [date] and [time] mean in [zone]. A time the clocks skipped moves forward.
 * A time before the tracker's earliest allowed one but in the same minute is that earliest time itself,
 * because the pickers have minute precision and a start has seconds.
 */
internal fun pickedInstant(
    tracker: Tracker,
    zone: ZoneId,
    date: LocalDate,
    time: LocalTime,
): Instant {
    val picked = ZonedDateTime.of(date, time, zone).toInstant()
    val earliest = tracker.earliestAllowedEventTime ?: return picked
    val sameMinuteAsStart = picked.truncatedTo(ChronoUnit.MINUTES) == earliest.truncatedTo(ChronoUnit.MINUTES)
    return if (picked < earliest && sameMinuteAsStart) earliest else picked
}

/** Why [at] cannot be saved for this tracker at [now], or null when it can. */
internal fun Tracker.errorFor(
    at: Instant,
    now: Instant,
): EntryTimeError? =
    when (validateEventTime(at, earliestAllowedEventTime, now)) {
        EventTimeValidation.Valid -> null
        EventTimeValidation.InFuture -> EntryTimeError.IN_FUTURE
        EventTimeValidation.BeforeTrackerStart -> EntryTimeError.BEFORE_TRACKER_START
    }
