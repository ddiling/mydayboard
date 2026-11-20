package io.github.ddiling.mydayboard.feature.trackers.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.tracker.ObserveTrackerSummariesUseCase
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerSummary
import io.github.ddiling.mydayboard.feature.trackers.format.toIntSaturated
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Turns the shared tracker summaries into rows. Sharing stops 5 s after the screen goes away, which keeps the
 * data across a rotation but stops the minute ticker once the list is really gone.
 */
@HiltViewModel
internal class TrackersListViewModel
    @Inject
    constructor(
        observeSummaries: ObserveTrackerSummariesUseCase,
    ) : ViewModel() {
        val uiState: StateFlow<TrackersListUiState> =
            observeSummaries()
                .map { summaries ->
                    if (summaries.isEmpty()) {
                        TrackersListUiState.Empty
                    } else {
                        TrackersListUiState.Content(summaries.map { it.toItem() })
                    }
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                    TrackersListUiState.Loading,
                )

        private fun TrackerSummary.toItem(): TrackerListItem =
            when (this) {
                is TrackerSummary.Counter -> {
                    TrackerListItem.Counter(tracker.id, tracker.name, summary.todayCount, summary.sinceLast)
                }

                is TrackerSummary.DaysSince -> {
                    TrackerListItem.DaysSince(tracker.id, tracker.name, summary.currentDays.toIntSaturated())
                }
            }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
