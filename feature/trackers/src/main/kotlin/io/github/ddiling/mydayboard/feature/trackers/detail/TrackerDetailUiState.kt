package io.github.ddiling.mydayboard.feature.trackers.detail

import io.github.ddiling.mydayboard.core.domain.tracker.DayCount
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeEditorState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime

/** What the detail screen shows. [Loading] lasts until the first emission; [NotFound] is e.g. a deleted tracker. */
internal sealed interface TrackerDetailUiState {
    data object Loading : TrackerDetailUiState

    data object NotFound : TrackerDetailUiState

    /** An event counter, as display-ready values so a preview needs no repository or clock. */
    data class Counter(
        val name: String,
        val todayCount: Int,
        val sinceLast: Duration?, // null: no event yet
        val lastDays: List<DayCount>, // oldest first, the last one is today
        val trend: Trend,
        val history: List<HistoryDay>, // newest day first
        val pendingUndo: Undo?,
        val editor: EventTimeEditorState? = null, // null: the entry dialog is closed
    ) : TrackerDetailUiState

    /** A days-since tracker, as display-ready values: dates are local dates in the clock's zone. */
    data class DaysSince(
        val name: String,
        val currentDays: Long,
        val currentStart: LocalDate,
        val longestDays: Long,
        val brokenStreaks: List<BrokenStreak>, // newest first
        val pendingUndo: Undo?,
        val editor: EventTimeEditorState? = null, // null: the entry dialog is closed
    ) : TrackerDetailUiState
}

/** The events of one local calendar day; [entries] are newest first. */
internal data class HistoryDay(
    val date: LocalDate,
    val entries: List<HistoryEntry>,
)

/** One event of the history: its local [time] and the [eventId] a tap on the row opens for editing. */
internal data class HistoryEntry(
    val eventId: String,
    val time: LocalTime,
)

/**
 * A finished streak of a days-since tracker, from the day it started to the day a reset ended it.
 * [endedByEventId] is that reset, which a tap on the streak opens for editing.
 */
internal data class BrokenStreak(
    val start: LocalDate,
    val end: LocalDate,
    val days: Long,
    val endedByEventId: String,
)
