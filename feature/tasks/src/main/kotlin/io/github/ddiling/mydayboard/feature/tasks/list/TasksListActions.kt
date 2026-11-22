package io.github.ddiling.mydayboard.feature.tasks.list

import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer

/** Everything the user can do on the list; each goes to the caller, so the screen itself holds no logic. */
internal data class TasksListActions(
    val onBack: () -> Unit = {},
    val onAddTask: () -> Unit = {},
    val onTaskClick: (String) -> Unit = {},
    val onCategoryStats: (String) -> Unit = {},
    val onSwipeDone: (String) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: (UndoOffer) -> Unit = {},
)
