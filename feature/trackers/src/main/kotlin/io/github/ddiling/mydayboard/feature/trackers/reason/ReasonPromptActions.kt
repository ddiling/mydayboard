package io.github.ddiling.mydayboard.feature.trackers.reason

/** What the dialog reports; "wait" also covers a tap outside and Back, so all three save nothing. */
internal data class ReasonPromptActions(
    val onWait: () -> Unit = {},
    val onAnother: () -> Unit = {},
    val onLogAnyway: () -> Unit = {},
    val onAddFirstReason: (trackerId: String) -> Unit = {},
)
