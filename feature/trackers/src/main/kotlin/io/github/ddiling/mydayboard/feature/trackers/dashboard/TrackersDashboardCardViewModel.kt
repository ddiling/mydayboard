package io.github.ddiling.mydayboard.feature.trackers.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.tracker.ObserveTrackerSummariesUseCase
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerSummary
import io.github.ddiling.mydayboard.feature.trackers.detail.QuickActionUndo
import io.github.ddiling.mydayboard.feature.trackers.format.toIntSaturated
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
import javax.inject.Inject

/**
 * Feeds the dashboard card: a row for each of the first [MAX_PLACES] trackers in the list's order (kept, so tiles
 * stay put), with a quick action (+1 or Reset) that can be undone. The
 * summaries come from the shared use case, which recomputes every minute so the day rolls over at midnight.
 * An action stamps the event with a fresh `clock.instant()`, never a tick's older instant, so the new event is
 * counted at once. Only the latest action is undoable, also across trackers, because the card has several
 * trackers but one snackbar. Sharing stops 5 s after the card goes away, which keeps the data across a rotation but
 * stops the minute ticker once the dashboard is really gone. A tracker that asks for a reason shows the reason
 * dialog first and logs only when the user chooses "Log anyway" ([onPromptLogAnyway]).
 */
@HiltViewModel
internal class TrackersDashboardCardViewModel
    @Inject
    constructor(
        observeSummaries: ObserveTrackerSummariesUseCase,
        private val repository: TrackerRepository,
        private val clock: Clock,
        private val prompter: ReasonPrompter,
    ) : ViewModel() {
        private val quickUndo = QuickActionUndo(repository::deleteEvent, repository::restoreEvent)

        val uiState: StateFlow<TrackersDashboardCardUiState> =
            combine(observeSummaries(), quickUndo.pending) { summaries, pendingUndo ->
                TrackersDashboardCardUiState.Content(
                    rows = summaries.take(MAX_PLACES).map { it.toRow() },
                    totalCount = summaries.size,
                    pendingUndo = pendingUndo,
                )
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                TrackersDashboardCardUiState.Loading,
            )

        /** The reason dialog to show, or null. */
        val prompt: StateFlow<ReasonPrompt?> = prompter.prompt

        fun onPlusOne(trackerId: String) = addEvent(trackerId, ReasonPromptKind.COUNTER)

        /** A reset is an event of a days-since tracker, so undoing it deletes that event like an undone +1. */
        fun onReset(trackerId: String) = addEvent(trackerId, ReasonPromptKind.DAYS_SINCE)

        /** "I'll wait", a tap outside and Back: nothing is saved. */
        fun onPromptWait() = prompter.dismiss()

        fun onPromptAnother() = prompter.another()

        /** Leaving for the reasons screen: the dialog closes and a pending Undo goes, like any other navigation. */
        fun onPromptAddFirstReason() {
            prompter.dismiss()
            quickUndo.dismiss()
        }

        fun onPromptLogAnyway() {
            prompter.confirm()?.let { log(it.trackerId, it.kind) }
        }

        fun onUndo() {
            viewModelScope.launch { quickUndo.undo() }
        }

        fun onUndoDismissed() = quickUndo.dismiss()

        private fun addEvent(
            trackerId: String,
            kind: ReasonPromptKind,
        ) {
            viewModelScope.launch {
                if (!prompter.request(trackerId, kind)) log(trackerId, kind)
            }
        }

        /** Stamps the event now, when the user chose to log, not when they tapped. */
        private fun log(
            trackerId: String,
            kind: ReasonPromptKind,
        ) {
            viewModelScope.launch {
                repository.addEvent(trackerId, clock.instant())?.let { quickUndo.register(it.id, kind.toUndoKind()) }
            }
        }

        private fun TrackerSummary.toRow(): TrackersDashboardRow =
            when (this) {
                is TrackerSummary.Counter -> {
                    TrackersDashboardRow.Counter(tracker.id, tracker.name, summary.todayCount, summary.sinceLast)
                }

                is TrackerSummary.DaysSince -> {
                    TrackersDashboardRow.DaysSince(tracker.id, tracker.name, summary.currentDays.toIntSaturated())
                }
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
