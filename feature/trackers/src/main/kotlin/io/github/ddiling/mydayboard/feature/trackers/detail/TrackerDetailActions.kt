package io.github.ddiling.mydayboard.feature.trackers.detail

import io.github.ddiling.mydayboard.feature.trackers.event.EventTimeDialogActions

/** What the detail screen reports to its caller; grouped so the screen's parameter list stays short. */
internal data class TrackerDetailActions(
    val onPlusOne: () -> Unit = {},
    val onReset: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onBack: () -> Unit = {},
    val onAddPastEntry: () -> Unit = {},
    val onEditEntry: (eventId: String) -> Unit = {},
    val entryDialog: EventTimeDialogActions = EventTimeDialogActions(),
)
