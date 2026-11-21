package io.github.ddiling.mydayboard.feature.trackers.reason

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoKind
import kotlin.random.Random

/** Which action the dialog guards, so it words its buttons ("Log anyway" or "Reset anyway") and the log matches. */
internal enum class ReasonPromptKind {
    /** A +1 on an event counter. */
    COUNTER,

    /** A reset of a days-since tracker. */
    DAYS_SINCE,
}

/** What the undo snackbar calls the action that [ReasonPromptKind] guards. */
internal fun ReasonPromptKind.toUndoKind(): UndoKind =
    when (this) {
        ReasonPromptKind.COUNTER -> UndoKind.ADDED
        ReasonPromptKind.DAYS_SINCE -> UndoKind.RESET
    }

/** The open dialog. It holds the tracker's name, not only its id, so the dialog needs no lookup to render. */
internal sealed interface ReasonPrompt {
    val trackerId: String
    val trackerName: String
    val kind: ReasonPromptKind

    /** [canSwap] is true only when the tracker has another reason to show. */
    data class WithReason(
        override val trackerId: String,
        override val trackerName: String,
        override val kind: ReasonPromptKind,
        val reason: TrackerReason,
        val canSwap: Boolean,
    ) : ReasonPrompt

    /** The switch is on but nothing is written yet, so the dialog invites the user to add a first reason. */
    data class NoReasons(
        override val trackerId: String,
        override val trackerName: String,
        override val kind: ReasonPromptKind,
    ) : ReasonPrompt
}

/**
 * Picks the reason to show: null for none, the only one when there is one, otherwise a random one that is not
 * [currentId], because "Another reason" that shows the same reason again would feel broken.
 */
internal fun pickReason(
    reasons: List<TrackerReason>,
    currentId: String?,
    random: Random,
): TrackerReason? {
    val candidates = if (reasons.size > 1) reasons.filter { it.id != currentId } else reasons
    return candidates.randomOrNull(random)
}
