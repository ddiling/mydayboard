package io.github.ddiling.mydayboard.feature.trackers.event

import java.time.LocalDate
import java.time.LocalTime

/** What the entry dialog reports; grouped so its parameter list stays short. */
internal data class EventTimeDialogActions(
    val onDateChange: (LocalDate) -> Unit = {},
    val onTimeChange: (LocalTime) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onDismiss: () -> Unit = {},
)
