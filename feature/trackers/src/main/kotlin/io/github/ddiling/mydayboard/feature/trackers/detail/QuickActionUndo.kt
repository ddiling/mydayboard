package io.github.ddiling.mydayboard.feature.trackers.detail

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.getAndUpdate

/** What a quick action did, so the snackbar can word the message. */
internal enum class UndoKind {
    /** A new event was added. */
    ADDED,

    /** A days-since tracker was reset, which recorded an event that starts the new streak. */
    RESET,
}

/** The one action that can still be undone: [eventId] is the event that action created. */
internal data class Undo(
    val eventId: String,
    val kind: UndoKind,
)

/**
 * Remembers the latest quick action so it can be undone, and only that one: a new action replaces the old one,
 * because an undo that reaches back past newer actions would surprise the user. Undo soft-deletes just the event
 * the action created, so it never touches events added by anything else. It lives apart from any screen so a
 * dashboard card can reuse it.
 */
internal class QuickActionUndo(
    private val deleteEvent: suspend (eventId: String) -> Unit,
) {
    private val _pending = MutableStateFlow<Undo?>(null)

    /** The undoable action, or null when there is none. */
    val pending: StateFlow<Undo?> = _pending.asStateFlow()

    /** Makes the [kind] action that created [eventId] the one that can be undone, dropping the previous one. */
    fun register(
        eventId: String,
        kind: UndoKind,
    ) {
        _pending.value = Undo(eventId, kind)
    }

    /**
     * Deletes the pending action's event. The pending value is cleared first, so a snackbar that reports
     * "dismissed" right after the tap, or a second tap, cannot delete twice.
     */
    suspend fun undo() {
        val undo = _pending.getAndUpdate { null } ?: return
        deleteEvent(undo.eventId)
    }

    /** Forgets the pending action without deleting anything, e.g. when the snackbar timed out. */
    fun dismiss() {
        _pending.value = null
    }
}
