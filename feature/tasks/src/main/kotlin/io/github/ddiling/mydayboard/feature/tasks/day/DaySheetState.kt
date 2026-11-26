package io.github.ddiling.mydayboard.feature.tasks.day

import java.time.LocalDate
import java.time.LocalTime

/**
 * The draft of one past day while the sheet is open. Nothing is stored until Save, so closing the sheet changes
 * nothing and one Undo reverts the whole edit. [taskName] is kept so the message can still name the task when the
 * task changes later.
 */
internal data class DaySheetState(
    val taskId: String,
    val taskName: String,
    val day: LocalDate,
    val kind: DaySheetKind,
)

/** What the sheet asks for, by the kind of task: a yes/no, the times of the day, or a count up to the goal. */
internal sealed interface DaySheetKind {
    data class Single(
        val done: Boolean,
    ) : DaySheetKind

    /** [slots] are all the task's times in order; [chosen] are the ticked ones. */
    data class Times(
        val slots: List<LocalTime>,
        val chosen: Set<LocalTime>,
    ) : DaySheetKind

    /** [value] always stays between 0 and [target]. */
    data class Count(
        val target: Int,
        val value: Int,
    ) : DaySheetKind
}
