package io.github.ddiling.mydayboard.core.domain.tracker

import java.time.Instant

/**
 * The kind of tracker the user created. Both kinds store the same [TrackerEvent]s; the type only decides
 * which statistics the app computes and shows for them.
 */
enum class TrackerType {
    /** Counts how often something happens: tap +1, see today's count, time since the last tap and a trend. */
    EVENT_COUNTER,

    /** Counts days since something last happened: each event ends the current streak and starts a new one. */
    DAYS_SINCE,
}

/**
 * A tracker the user configured in the app. The app ships no built-in trackers: every tracker, including its
 * [name], comes from the user.
 *
 * All instants are UTC; a time zone is applied only when a value is read for display or grouped into days, so
 * moving to another zone never rewrites stored data.
 *
 * @property id Stable UUID, so a future cloud sync can match the same tracker across devices.
 * @property name What the user called it, already validated by [validateTrackerName]. Never translated.
 * @property type Which statistics the tracker shows.
 * @property startedAt When tracking began. For [TrackerType.DAYS_SINCE] it is the start of the first streak,
 *   before any event exists. For [TrackerType.EVENT_COUNTER] it only decides whether there is enough history
 *   to compute a [Trend]: without it, a tracker created yesterday would look like it had two quiet weeks.
 * @property createdAt When the row was created, kept for sync.
 * @property updatedAt When the row last changed, so sync can keep the newest version.
 * @property askForReason When on, logging an event first shows one of the tracker's reasons, to give the user a
 *   moment to think before they continue.
 */
data class Tracker(
    val id: String,
    val name: String,
    val type: TrackerType,
    val startedAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
    val askForReason: Boolean = false,
)

/**
 * A short sentence the user wrote for a tracker, shown when [Tracker.askForReason] is on.
 *
 * @property id Stable UUID, for sync.
 * @property trackerId The [Tracker.id] this reason belongs to.
 * @property text What the user wrote, already validated by [validateReasonText].
 * @property createdAt When the row was created; reasons are listed oldest first by it.
 * @property updatedAt When the row last changed.
 */
data class TrackerReason(
    val id: String,
    val trackerId: String,
    val text: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * One occurrence recorded for a tracker (one tap on an event counter, one reset of a days-since tracker).
 *
 * [occurredAt] and [createdAt] are separate because the user may record an event after the fact: the
 * statistics use [occurredAt], sync uses [createdAt] and [updatedAt].
 *
 * @property id Stable UUID, for sync.
 * @property trackerId The [Tracker.id] this event belongs to.
 * @property occurredAt When the event happened, in UTC.
 * @property createdAt When the row was created.
 * @property updatedAt When the row last changed (for example the user corrected [occurredAt]).
 */
data class TrackerEvent(
    val id: String,
    val trackerId: String,
    val occurredAt: Instant,
    val createdAt: Instant,
    val updatedAt: Instant,
)

/**
 * The earliest time an event of this tracker may have, or null when there is no limit. An event before the
 * start of a [TrackerType.DAYS_SINCE] tracker would make a streak start before tracking began. An
 * [TrackerType.EVENT_COUNTER] accepts past events that predate creating the tracker, because the user may
 * be catching up on history.
 */
val Tracker.earliestAllowedEventTime: Instant?
    get() =
        when (type) {
            TrackerType.DAYS_SINCE -> startedAt
            TrackerType.EVENT_COUNTER -> null
        }
