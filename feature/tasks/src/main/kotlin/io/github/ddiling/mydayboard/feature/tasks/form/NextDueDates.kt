package io.github.ddiling.mydayboard.feature.tasks.form

import io.github.ddiling.mydayboard.core.domain.task.EveryNDaysMode
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.nextDueDate
import java.time.Instant
import java.time.LocalDate

/** How many upcoming dates the "every N days" info line lists: enough to see the rhythm, short enough for one line. */
internal const val NEXT_DATES_SHOWN = 3

/**
 * The next due dates of the "every N days" task being edited, so the user sees what the interval and the mode
 * mean before saving. The first date comes from the same `nextDueDate` the lists use (one source of truth);
 * with no ticks yet every later date is one interval after the previous one, in both modes. [TaskFormUiState.today]
 * comes from the injected clock, so the dates follow the clock's zone. Empty for any other repeat type, and for a
 * saved task counted from the last time: its real dates depend on the last completion, which the form does not know.

 */
internal fun TaskFormUiState.nextDueDates(count: Int = NEXT_DATES_SHOWN): List<LocalDate> {
    val knowsItsDates =
        repeatType == RepeatType.EVERY_N_DAYS && !(isEditing && intervalMode == EveryNDaysMode.FROM_LAST_DONE)
    val first = if (knowsItsDates) firstDueDate() else null
    return first?.let { start -> List(count) { start.plusDays(it.toLong() * intervalDays) } }.orEmpty()
}

private fun TaskFormUiState.firstDueDate(): LocalDate? {
    val preview =
        Task(
            id = "",
            name = name,
            categoryId = null,
            repeat = Repeat.EveryNDays(intervalDays, intervalMode, firstDue),
            time = null,
            note = null,
            goal = null,
            onMiss = OnMiss.EXPIRES,
            startDate = today,
            archivedAt = null,
            createdAt = Instant.EPOCH,
            updatedAt = Instant.EPOCH,
        )
    return nextDueDate(preview, emptyList(), today)
}
