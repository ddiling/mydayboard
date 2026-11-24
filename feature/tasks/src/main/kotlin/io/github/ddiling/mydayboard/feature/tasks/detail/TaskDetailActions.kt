package io.github.ddiling.mydayboard.feature.tasks.detail

import java.time.LocalDate
import java.time.LocalTime

/** Everything the user can do on the details; each goes to the caller, so the screen itself holds no logic. */
internal data class TaskDetailActions(
    val onBack: () -> Unit = {},
    val onEdit: () -> Unit = {},
    val onBigButton: () -> Unit = {},
    val onChipClick: (LocalTime) -> Unit = {},
    val onHistoryRowClick: (LocalDate) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
)
