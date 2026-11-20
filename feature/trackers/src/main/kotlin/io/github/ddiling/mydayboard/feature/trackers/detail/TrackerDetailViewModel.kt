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
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerDetailDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/**
 * Shows one tracker and lets the user add events to it. "Today" is the local calendar day in the clock's
 * current zone, and the state is recomputed every minute, so the count resets after midnight and "since last"
 * keeps moving without any user action. Sharing stops 5 s after the screen goes away, which keeps the data
 * across a rotation but stops the minute ticker once the screen is really gone.
 */
@HiltViewModel
internal class TrackerDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TrackerRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val trackerId: String = savedStateHandle.toRoute<TrackerDetailDestination>().trackerId
        private val quickUndo = QuickActionUndo(repository::deleteEvent)

        val uiState: StateFlow<TrackerDetailUiState> =
            combine(
                repository.observeTracker(trackerId),
                repository.observeEvents(trackerId),
                minuteTicker(clock),
                quickUndo.pending,
            ) { tracker, events, now, pendingUndo ->
                if (tracker == null) {
                    TrackerDetailUiState.NotFound
                } else {
                    when (tracker.type) {
                        TrackerType.EVENT_COUNTER -> counterState(tracker, events, now, pendingUndo)
                        TrackerType.DAYS_SINCE -> TrackerDetailUiState.DaysSince(tracker.name)
                    }
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                TrackerDetailUiState.Loading,
            )

        fun onPlusOne() {
            viewModelScope.launch {
                repository.addEvent(trackerId, clock.instant())?.let { quickUndo.register(it.id) }
            }
        }

        fun onUndo() {
            viewModelScope.launch { quickUndo.undo() }
        }

        fun onUndoDismissed() = quickUndo.dismiss()

        private fun counterState(
            tracker: Tracker,
            events: List<TrackerEvent>,
            now: Instant,
            pendingUndo: Undo?,
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
            )
        }

        private fun List<TrackerEvent>.toHistory(): List<HistoryDay> {
            val zone = clock.zone
            // Sorting by instant first keeps a repeated local hour (clocks going back) in the right order.
            return sortedByDescending { it.occurredAt }
                .map { it.occurredAt.atZone(zone) }
                .groupBy { it.toLocalDate() }
                .map { (date, moments) -> HistoryDay(date, moments.map { it.toLocalTime() }) }
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
            const val LAST_DAYS = 7
        }
    }
