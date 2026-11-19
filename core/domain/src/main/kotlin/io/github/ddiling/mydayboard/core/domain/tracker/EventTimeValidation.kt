package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Instant

/**
 * Whether a time the user picked for a tracker entry can be saved. The UI maps each problem to its own
 * message, so the cases say what is wrong rather than carrying text.
 */
sealed interface EventTimeValidation {
    /** The time can be saved. */
    data object Valid : EventTimeValidation

    /** The time is after now: an entry records something that already happened. */
    data object InFuture : EventTimeValidation

    /** The time is before the earliest allowed one, for example before a "days since" tracker started. */
    data object BeforeTrackerStart : EventTimeValidation
}

/**
 * Checks a custom entry time [occurredAt] before it is saved.
 *
 * A time equal to [now] or to [earliestAllowed] is valid: both bounds are inclusive, so "right now"
 * and "at the moment the tracker started" can be recorded.
 *
 * [earliestAllowed] is the lower bound, or `null` for none. Taking the bound instead of a tracker keeps
 * this rule independent of the tracker types: a "days since" tracker passes its start (a reset before
 * the start would make no sense), a counter passes `null`, because past counts can be entered at any time.
 *
 * When the time is both in the future and before the bound (only possible when the bound itself is in
 * the future), [EventTimeValidation.InFuture] wins. "Not in the future" applies to every tracker and
 * must hold for any valid time, so it is the message that always points the user towards a fix; moving
 * the time after the bound could still leave it in the future.
 *
 * [now] is a parameter, never read from the system, so the check is deterministic and testable.
 */
fun validateEventTime(
    occurredAt: Instant,
    earliestAllowed: Instant?,
    now: Instant,
): EventTimeValidation =
    when {
        occurredAt.isAfter(now) -> EventTimeValidation.InFuture
        earliestAllowed != null && occurredAt.isBefore(earliestAllowed) -> EventTimeValidation.BeforeTrackerStart
        else -> EventTimeValidation.Valid
    }
