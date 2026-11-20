package io.github.ddiling.mydayboard.feature.trackers.editor

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import java.time.LocalDate

/** What the editor reports to its caller; grouped so the screen's parameter list stays short. */
internal data class TrackerEditorActions(
    val onNameChange: (String) -> Unit = {},
    val onTypeChange: (TrackerType) -> Unit = {},
    val onStartDateChange: (LocalDate) -> Unit = {},
    val onSave: () -> Unit = {},
    val onDeleteClick: () -> Unit = {},
    val onDeleteConfirm: () -> Unit = {},
    val onDeleteDismiss: () -> Unit = {},
    val onBack: () -> Unit = {},
)
