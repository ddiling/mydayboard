package io.github.ddiling.mydayboard.feature.trackers.event

import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * Adds an entry at a past time, changes the time of an entry, or deletes one, for the tracker [trackerId].
 * It lives apart from the ViewModel so that one stays small; the ViewModel owns it and passes its scope.
 *
 * The repository does not validate, so every change is checked here with [validateEventTime] against a fresh
 * `clock.instant()` (a stale "now" would accept a future time) and the tracker as stored at that moment. The
 * error is shown live, and saving does nothing while it is set. The picked local date and time become an
 * instant in the clock's current zone; a time that does not exist (the clocks skipped it) moves forward, so
 * 02:30 on the spring change day in Warsaw is stored as 03:30.
 *
 * The pickers have minute precision but a tracker's start has seconds. A time before the start but in the
 * same minute is therefore stored as the start itself, so the user can pick "the start minute".
 *
 * [onDeleted] gets the id of a deleted entry, so the caller can offer Undo.
 */
internal class EventTimeEditor(
    private val scope: CoroutineScope,
    private val trackerId: String,
    private val repository: TrackerRepository,
    private val clock: Clock,
    private val onDeleted: (eventId: String) -> Unit,
) {
    private val _state = MutableStateFlow<EventTimeEditorState?>(null)

    /** The open dialog, or null when it is closed. */
    val state: StateFlow<EventTimeEditorState?> = _state.asStateFlow()

    /** Opens the dialog to add an entry, starting at the current minute. */
    fun openAdd() {
        scope.launch {
            val tracker = loadTracker() ?: return@launch
            val now = clock.instant().atZone(clock.zone).truncatedTo(ChronoUnit.MINUTES)
            show(tracker, EntryMode.Add, now.toLocalDate(), now.toLocalTime())
        }
    }

    /** Opens the dialog for the entry [eventId], showing its current local date and time. Unknown ids do nothing. */
    fun openEdit(eventId: String) {
        scope.launch {
            val tracker = loadTracker() ?: return@launch
            val event = repository.observeEvents(trackerId).first().firstOrNull { it.id == eventId } ?: return@launch
            val local = event.occurredAt.atZone(clock.zone).truncatedTo(ChronoUnit.MINUTES)
            show(tracker, EntryMode.Edit(eventId), local.toLocalDate(), local.toLocalTime())
        }
    }

    fun changeDate(date: LocalDate) = change { it.copy(date = date) }

    fun changeTime(time: LocalTime) = change { it.copy(time = time) }

    /** Stores the entry and closes the dialog, unless the dialog is closed or the picked time is invalid. */
    fun save() {
        scope.launch {
            val tracker = loadTracker() ?: return@launch
            val current = _state.value ?: return@launch
            val at = pickedInstant(tracker, clock.zone, current.date, current.time)
            if (tracker.errorFor(at, clock.instant()) != null || !_state.compareAndSet(current, null)) return@launch
            when (val mode = current.mode) {
                EntryMode.Add -> repository.addEvent(trackerId, at)
                is EntryMode.Edit -> repository.updateEventTime(mode.eventId, at)
            }
        }
    }

    /** Deletes the entry being edited and closes the dialog. An add dialog has nothing to delete. */
    fun delete() {
        scope.launch {
            val current = _state.value ?: return@launch
            val mode = current.mode as? EntryMode.Edit ?: return@launch
            if (!_state.compareAndSet(current, null)) return@launch
            repository.deleteEvent(mode.eventId)
            onDeleted(mode.eventId)
        }
    }

    /** Closes the dialog without saving. */
    fun dismiss() {
        _state.value = null
    }

    private suspend fun loadTracker(): Tracker? = repository.observeTracker(trackerId).first()

    private fun show(
        tracker: Tracker,
        mode: EntryMode,
        date: LocalDate,
        time: LocalTime,
    ) {
        _state.value = EventTimeEditorState(mode, date, time, errorOf(tracker, date, time))
    }

    private fun change(transform: (EventTimeEditorState) -> EventTimeEditorState) {
        scope.launch {
            val tracker = loadTracker() ?: return@launch
            _state.update { current ->
                current?.let(transform)?.let { next ->
                    next.copy(error = errorOf(tracker, next.date, next.time))
                }
            }
        }
    }

    private fun errorOf(
        tracker: Tracker,
        date: LocalDate,
        time: LocalTime,
    ): EntryTimeError? = tracker.errorFor(pickedInstant(tracker, clock.zone, date, time), clock.instant())
}
