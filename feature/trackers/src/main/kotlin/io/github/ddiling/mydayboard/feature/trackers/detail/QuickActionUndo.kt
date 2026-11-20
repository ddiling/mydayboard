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

    /** An entry was deleted from the history. */
    DELETED,
}

/** The one action that can still be undone: [eventId] is the event that action created or deleted. */
internal data class Undo(
    val eventId: String,
    val kind: UndoKind,
)

/**
 * Remembers the latest quick action so it can be undone, and only that one: a new action replaces the old one,
 * because an undo that reaches back past newer actions would surprise the user. Undo touches just that action's
 * event, so it never affects events added by anything else: it deletes the event an add or reset created, and
 * restores the event a delete removed. It lives apart from any screen so a dashboard card can reuse it.
 */
internal class QuickActionUndo(
    private val deleteEvent: suspend (eventId: String) -> Unit,
    private val restoreEvent: suspend (eventId: String) -> Unit,
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
     * Reverses the pending action: deletes its event for [UndoKind.ADDED] and [UndoKind.RESET], restores it for
     * [UndoKind.DELETED]. The pending value is cleared first, so a snackbar that reports "dismissed" right after
     * the tap, or a second tap, cannot act twice.
     */
    suspend fun undo() {
        val undo = _pending.getAndUpdate { null } ?: return
        when (undo.kind) {
            UndoKind.ADDED, UndoKind.RESET -> deleteEvent(undo.eventId)
            UndoKind.DELETED -> restoreEvent(undo.eventId)
        }
    }

    /** Forgets the pending action without changing any event, e.g. when the snackbar timed out. */
    fun dismiss() {
        _pending.value = null
    }
}
