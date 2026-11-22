package io.github.ddiling.mydayboard.feature.tasks.list

import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import java.time.LocalDate

/**
 * Message after a swipe: the task's [name] and what the swipe did, so the snackbar can say "Send CVs 2/3" or
 * "Breakfast done" and hand [outcome] back to Undo. The name is kept here because the row may be gone by the
 * time the message shows.
 */
internal data class UndoOffer(
    val name: String,
    val outcome: TickOutcome.Added,
) {
    /** True when this swipe finished the task (all counts or all times done). */
    val completes: Boolean get() = outcome.done >= outcome.target
}

internal sealed interface TasksListUiState {
    data object Loading : TasksListUiState

    /** No live task at all: the screen invites the user to add one. */
    data object Empty : TasksListUiState

    /** [today] is the day the rows were judged on, which the late text needs to say "was due Fri". */
    data class Content(
        val groups: List<TaskGroup>,
        val today: LocalDate,
        val lateCount: Int,
        val archivedCount: Int,
        val undo: UndoOffer?,
    ) : TasksListUiState
}
