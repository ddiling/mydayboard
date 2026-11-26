package io.github.ddiling.mydayboard.feature.tasks.list

import io.github.ddiling.mydayboard.core.domain.task.TaskGroup
import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer
import java.time.LocalDate

internal sealed interface TasksListUiState {
    data object Loading : TasksListUiState

    /** No task at all, archived ones included: the screen invites the user to add one. */
    data object Empty : TasksListUiState

    /**
     * [today] is the day the rows were judged on, which the late text needs to say "was due Fri".
     * [groups] may be empty while [archivedCount] is not: then only the Archived row is shown.
     */
    data class Content(
        val groups: List<TaskGroup>,
        val today: LocalDate,
        val lateCount: Int,
        val archivedCount: Int,
        val undo: UndoOffer?,
    ) : TasksListUiState
}
