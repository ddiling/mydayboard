package io.github.ddiling.mydayboard.feature.trackers.reasons

import io.github.ddiling.mydayboard.core.domain.tracker.MAX_REASON_LENGTH
import io.github.ddiling.mydayboard.core.domain.tracker.ReasonTextValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.validateReasonText

/** What the reasons screen shows. [Loading] lasts until the first emission, so "empty" never flashes on start. */
internal sealed interface TrackerReasonsUiState {
    data object Loading : TrackerReasonsUiState

    data object NotFound : TrackerReasonsUiState

    /**
     * [reasons] are oldest first; an empty list is the empty state. [pendingUndoId] is the one deleted reason
     * that can still be brought back, and [editor] is the open add / edit dialog, if any.
     */
    data class Content(
        val trackerName: String,
        val reasons: List<TrackerReason>,
        val editor: ReasonEditor? = null,
        val pendingUndoId: String? = null,
    ) : TrackerReasonsUiState
}

/**
 * The add / edit dialog: [reasonId] is null when adding. The checks live in the domain, so the limit is
 * defined once; the dialog only shows what [validation] says.
 */
internal data class ReasonEditor(
    val reasonId: String?,
    val text: String,
) {
    val validation: ReasonTextValidation get() = validateReasonText(text)

    /** Save is offered only for text that would be stored, so blank and too-long text never gets that far. */
    val canSave: Boolean get() = validation is ReasonTextValidation.Valid

    /** Length that counts against the limit: the ends are trimmed, as they are when the reason is stored. */
    val length: Int get() = text.trim().length

    val maxLength: Int get() = MAX_REASON_LENGTH
}
