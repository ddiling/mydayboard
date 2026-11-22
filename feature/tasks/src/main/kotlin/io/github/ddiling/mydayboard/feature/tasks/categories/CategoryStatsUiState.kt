package io.github.ddiling.mydayboard.feature.tasks.categories

internal sealed interface CategoryStatsUiState {
    data object Loading : CategoryStatsUiState

    /** The category was deleted or never existed. */
    data object NotFound : CategoryStatsUiState

    /** The category exists but holds no task, archived ones included. */
    data class Empty(
        val categoryName: String,
    ) : CategoryStatsUiState

    /**
     * Numbers as data, so the screen formats them in the device language. They cover all the tasks of the category
     * over one 30-day window, archived tasks included up to the day they were archived, so [archivedLines] are
     * listed apart but still add to [perDay], [doneOfPlanned], [plannedDays] and [skipped].
     */
    data class Content(
        val categoryName: String,
        val perDay: Double,
        val doneOfPlanned: Double,
        val plannedDays: Int,
        val skipped: Int,
        val tasks: List<TaskLine>,
        val archivedLines: List<TaskLine>,
    ) : CategoryStatsUiState
}

/** One task in the lists; [skipped] is the task's own count of planned days with no credit. */
internal data class TaskLine(
    val id: String,
    val name: String,
    val skipped: Int,
)
