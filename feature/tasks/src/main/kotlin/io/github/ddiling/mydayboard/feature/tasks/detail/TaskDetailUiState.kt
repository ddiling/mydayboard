package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer
import io.github.ddiling.mydayboard.feature.tasks.day.DayMarked
import java.time.LocalDate
import java.time.LocalTime

internal sealed interface TaskDetailUiState {
    data object Loading : TaskDetailUiState

    /** The task was deleted, archived, or never existed. */
    data object NotFound : TaskDetailUiState

    /**
     * Everything the screen draws, as data and not as text, so the screen only formats it in the device language.
     * [today] is the day it was judged on, which "Late since Fri" and "Next: Sat 28.11" need. [nextDue] is set only
     * while the task is not due today; it comes with [BigButton.None].
     */
    data class Content(
        val task: Task,
        val categoryName: String?,
        val today: LocalDate,
        val hero: Hero,
        val stats: DetailStats,
        val chips: List<TimeChip>,
        val history: List<HistoryRow>,
        val bigButton: BigButton,
        val nextDue: LocalDate?,
        val pendingUndo: DetailUndo?,
    ) : TaskDetailUiState
}

/** The big status word at the top. */
internal sealed interface Hero {
    data object NotDone : Hero

    data object DoneToday : Hero

    /** Some of a several-times task is ticked: "1 of 2 today". */
    data class TimesProgress(
        val done: Int,
        val target: Int,
    ) : Hero

    /** Some of a count goal is ticked: "2/3 today". */
    data class CountProgress(
        val done: Int,
        val target: Int,
    ) : Hero

    data class LateSince(
        val day: LocalDate,
    ) : Hero

    /** Not due today; the next day comes from [TaskDetailUiState.Content.nextDue]. */
    data object NotDueToday : Hero

    /** A one-time task with nothing left: "Done" when [reached], otherwise "Missed". */
    data class Finished(
        val reached: Boolean,
    ) : Hero
}

/** What the big button at the bottom does; one tap is one tick, one untick, or one +1. */
internal sealed interface BigButton {
    data object MarkDone : BigButton

    /** Ticks the next undone [time] of a several-times task. */
    data class TickNext(
        val time: LocalTime,
    ) : BigButton

    data object PlusOne : BigButton

    data object UntickToday : BigButton

    /** The task is not due today, so there is nothing to press. */
    data object None : BigButton
}

internal data class TimeChip(
    val time: LocalTime,
    val ticked: Boolean,
)

internal data class HistoryRow(
    val day: LocalDate,
    val result: HistoryResult,
)

internal sealed interface HistoryResult {
    data object Done : HistoryResult

    data object Missed : HistoryResult

    /** Overdue but still open: the hero and the big button let the user do it, so history must not call it missed. */
    data object Late : HistoryResult

    /**
     * Part of the target was reached. [settled] is false only for today, which can still be finished; a settled
     * one reads "half a day" ([severalTimes]) or "not reached" (a count goal).
     */
    data class Partial(
        val done: Int,
        val target: Int,
        val severalTimes: Boolean,
        val settled: Boolean,
    ) : HistoryResult
}

/** The numbers under the status word. A one-time task has only facts, because a streak means nothing for it. */
internal sealed interface DetailStats {
    val since: LocalDate

    data class Facts(
        override val since: LocalDate,
        val daysLate: Int?,
    ) : DetailStats

    /**
     * [perDay] counts raw ticks for a count goal ([countsTicks]) and credit otherwise. [halfDays] is null unless the
     * task repeats several times a day.
     */
    data class Recurring(
        override val since: LocalDate,
        val streak: Int,
        val doneOfPlanned: Double,
        val plannedDays: Int,
        val perDay: Double,
        val countsTicks: Boolean,
        val skipped: Int,
        val halfDays: Int?,
    ) : DetailStats
}

/** The message with Undo after a button or chip tap, or after a past day was saved. */
internal sealed interface DetailUndo {
    val outcome: TickOutcome

    data class Ticked(
        val offer: UndoOffer,
    ) : DetailUndo {
        override val outcome: TickOutcome get() = offer.outcome
    }

    /** [name] is kept so "Pills unticked" can still be said when the task changes. */
    data class Unticked(
        val name: String,
        override val outcome: TickOutcome.Removed,
    ) : DetailUndo

    /** A past day was rewritten from the day sheet; one Undo reverts all of it. */
    data class Marked(
        val marked: DayMarked,
    ) : DetailUndo {
        override val outcome: TickOutcome get() = marked.outcome
    }
}
