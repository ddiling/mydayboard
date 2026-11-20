package io.github.ddiling.mydayboard.feature.trackers.detail

/** What the detail screen reports to its caller; grouped so the screen's parameter list stays short. */
internal data class TrackerDetailActions(
    val onPlusOne: () -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
    val onEditClick: () -> Unit = {},
    val onBack: () -> Unit = {},
)
