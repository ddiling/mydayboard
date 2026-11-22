package io.github.ddiling.mydayboard.core.domain.task

import java.time.LocalDate
import java.time.LocalTime

/**
 * What a [TaskActions] call changed. The UI shows it as a message with Undo and hands it back to
 * [TaskActions.undo], so the outcome carries exactly what is needed to revert: ticks are only ever deleted and
 * restored, never rewritten.
 */
sealed interface TickOutcome {
    val taskId: String

    /** A tick was added; [done] of [target] is the progress after it, [slot] the time it ticked, if any. */
    data class Added(
        override val taskId: String,
        val tickId: String,
        val done: Int,
        val target: Int,
        val slot: LocalTime?,
    ) : TickOutcome

    /** A tick was removed; [done] of [target] is the progress after it. */
    data class Removed(
        override val taskId: String,
        val tickId: String,
        val done: Int,
        val target: Int,
    ) : TickOutcome

    /** A past [day] was rewritten; [change] is what [TaskTickRepository.undoDayChange] needs. */
    data class DayReplaced(
        override val taskId: String,
        val day: LocalDate,
        val change: DayChange,
    ) : TickOutcome
}
