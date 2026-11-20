package io.github.ddiling.mydayboard.feature.trackers.dashboard

/** What the card reports to its caller; grouped so the composable's parameter list stays short. */
internal data class TrackersDashboardCardActions(
    val onPlusOne: (trackerId: String) -> Unit = {},
    val onReset: (trackerId: String) -> Unit = {},
    val onUndo: () -> Unit = {},
    val onUndoDismissed: () -> Unit = {},
)
