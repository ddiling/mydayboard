package io.github.ddiling.mydayboard.feature.tasks.day

import java.time.LocalTime

/** What the sheet can do; each goes to the holder, so the content itself holds no logic. */
internal data class DaySheetActions(
    val onSetSingle: (Boolean) -> Unit = {},
    val onToggle: (LocalTime) -> Unit = {},
    val onStep: (Int) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)
