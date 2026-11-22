package io.github.ddiling.mydayboard.feature.tasks.categories

/** Everything the user can do on the category stats screen; each goes to the caller. */
internal data class CategoryStatsActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onTaskClick: (String) -> Unit = {},
)
