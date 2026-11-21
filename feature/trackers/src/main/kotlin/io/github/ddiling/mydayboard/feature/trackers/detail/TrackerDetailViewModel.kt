package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.time.minuteTicker
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.domain.tracker.counterTrend
import io.github.ddiling.mydayboard.core.domain.tracker.dailyCounts
import io.github.ddiling.mydayboard.core.domain.tracker.summarizeCounter
import io.github.ddiling.mydayboard.core.domain.tracker.summarizeDaysSince
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeEditor
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeEditorState
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPrompt
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptKind
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPrompter
import io.github.ddiling.mydayboard.feature.trackers.reason.toUndoKind
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Shows one tracker and lets the user add events to it, or change or delete them through [entryEditor].
 * "Today" is the local calendar day in the clock's current zone, and the state is recomputed every minute,
 * so the count resets after midnight and "since last" keeps moving without any user action. The ticker only
 * triggers the recomputation: every run reads a fresh `clock.instant()`, because the tick's own instant can
 * be up to a minute old and would hide an event the user just added (the summaries ignore events after
 * "now"). Sharing stops 5 s after the screen goes away, which keeps the data across a rotation but stops the
 * minute ticker once the screen is really gone. When the tracker asks for a reason, +1 and Reset show the
 * reason dialog first and log only on "Log anyway".
 */
@HiltViewModel
internal class TrackerDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TrackerRepository,
        private val clock: Clock,
        val prompter: ReasonPrompter,
    ) : ViewModel() {
        private val trackerId: String = savedStateHandle.toRoute<TrackerDetailDestination>().trackerId
        private val quickUndo = QuickActionUndo(repository::deleteEvent, repository::restoreEvent)

        /** The add / edit / delete entry dialog; the screen calls its actions and reads its state from [uiState]. */
        val entryEditor =
            EventTimeEditor(viewModelScope, trackerId, repository, clock) { quickUndo.register(it, UndoKind.DELETED) }

        val uiState: StateFlow<TrackerDetailUiState> =
            combine(
                repository.observeTracker(trackerId),
                repository.observeEvents(trackerId),
                minuteTicker(clock),
                quickUndo.pending,
                entryEditor.state,
            ) { tracker, events, _, pendingUndo, editor ->
                val now = clock.instant()
                if (tracker == null) {
                    TrackerDetailUiState.NotFound
                } else {
                    when (tracker.type) {
                        TrackerType.EVENT_COUNTER -> counterState(tracker, events, now, pendingUndo, editor)
                        TrackerType.DAYS_SINCE -> daysSinceState(tracker, events, now, pendingUndo, editor)
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                TrackerDetailUiState.Loading,
            )

        /** The reason dialog to show, or null. */
        val prompt: StateFlow<ReasonPrompt?> = prompter.prompt

        fun onPlusOne() = addEvent(ReasonPromptKind.COUNTER)

        /** A reset is an event of a days-since tracker, so undoing it deletes that event like an undone +1. */
        fun onReset() = addEvent(ReasonPromptKind.DAYS_SINCE)

        /** Wait, a tap outside and Back go straight to [prompter]: they save nothing, so need no logic here. */
        fun onPromptLogAnyway() {
            prompter.confirm()?.let { log(it.kind) }
        }

        private fun addEvent(kind: ReasonPromptKind) {
            viewModelScope.launch {
                if (!prompter.request(trackerId, kind)) log(kind)
            }
        }

        /** Stamps the event now, when the user chose to log, not when they tapped. */
        private fun log(kind: ReasonPromptKind) {
            viewModelScope.launch {
                repository.addEvent(trackerId, clock.instant())?.let { quickUndo.register(it.id, kind.toUndoKind()) }
            }
        }

        fun onUndo() {
            viewModelScope.launch { quickUndo.undo() }
        }

        fun onUndoDismissed() = quickUndo.dismiss()

        private fun daysSinceState(
            tracker: Tracker,
            events: List<TrackerEvent>,
            now: Instant,
            pendingUndo: Undo?,
            editor: EventTimeEditorState?,
        ): TrackerDetailUiState.DaysSince {
            val zone = clock.zone
            val summary = summarizeDaysSince(tracker.startedAt, events.map { it.occurredAt }, now, zone)
            // The summary ignores resets outside startedAt..now, so only those can have ended a streak. Resets at
            // the same instant are interchangeable, so the order among them does not matter.
            val resets = events.filter { it.occurredAt in tracker.startedAt..now }.sortedByDescending { it.occurredAt }
            return TrackerDetailUiState.DaysSince(
                name = tracker.name,
                currentDays = summary.currentDays,
                currentStart = summary.currentStart.atZone(zone).toLocalDate(),
                longestDays = summary.longestDays,
                // Both lists are newest first: the k-th broken streak was ended by the k-th newest reset.
                brokenStreaks =
                    summary.brokenStreaks.zip(resets).map { (streak, reset) ->
                        // A broken streak always has an end; the fallback only satisfies the nullable type.
                        BrokenStreak(
                            streak.start.atZone(zone).toLocalDate(),
                            (streak.end ?: streak.start).atZone(zone).toLocalDate(),
                            streak.days,
                            endedByEventId = reset.id,
                        )
                    },
                pendingUndo = pendingUndo,
                editor = editor,
            )
        }

        private fun counterState(
            tracker: Tracker,
            events: List<TrackerEvent>,
            now: Instant,
            pendingUndo: Undo?,
            editor: EventTimeEditorState?,
        ): TrackerDetailUiState.Counter {
            val zone = clock.zone
            val today = now.atZone(zone).toLocalDate()
            val times = events.map { it.occurredAt }
            val summary = summarizeCounter(times, now, zone)
            // A backdated event can predate the start date; the history then really begins at that event.
            val historyStart = minOf(tracker.startedAt, times.minOrNull() ?: tracker.startedAt)
            return TrackerDetailUiState.Counter(
                name = tracker.name,
                todayCount = summary.todayCount,
                sinceLast = summary.sinceLast,
                lastDays = dailyCounts(times, today, days = LAST_DAYS, zone = zone),
                trend = counterTrend(times, historyStart, today, zone),
                history = events.toHistory(),
                pendingUndo = pendingUndo,
                editor = editor,
            )
        }

        private fun List<TrackerEvent>.toHistory(): List<HistoryDay> {
            val zone = clock.zone
            // Sorting by instant first keeps a repeated local hour (clocks going back) in the right order.
            return sortedByDescending { it.occurredAt }
                .groupBy { it.occurredAt.atZone(zone).toLocalDate() }
                .map { (date, day) ->
                    HistoryDay(date, day.map { HistoryEntry(it.id, it.occurredAt.atZone(zone).toLocalTime()) })
                }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val LAST_DAYS = 7
        }
    }
