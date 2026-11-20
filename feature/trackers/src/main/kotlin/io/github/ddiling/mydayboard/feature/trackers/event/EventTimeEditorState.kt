package io.github.ddiling.mydayboard.feature.trackers.event

import java.time.LocalDate
import java.time.LocalTime

/** Whether the dialog adds a new entry or changes the time of an existing one. */
internal sealed interface EntryMode {
    data object Add : EntryMode

    data class Edit(
        val eventId: String,
    ) : EntryMode
}

/** Why the picked time cannot be saved. */
internal enum class EntryTimeError {
    /** The time is after now: an entry records something that already happened. */
    IN_FUTURE,

    /** The time is before a days-since tracker started. */
    BEFORE_TRACKER_START,
}

/**
 * The open entry dialog: the picked local [date] and [time] (minute precision, in the clock's zone) and the
 * [error] they currently have, if any.
 */
internal data class EventTimeEditorState(
    val mode: EntryMode,
    val date: LocalDate,
    val time: LocalTime,
    val error: EntryTimeError?,
) {
    val canSave: Boolean get() = error == null
}
