package io.github.ddiling.mydayboard.feature.trackers.editor

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerNameValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.domain.tracker.validateTrackerName
import java.time.LocalDate

/**
 * How the editor was left, so navigation can react and the screen does not need to know the routes. [Saved]
 * carries the id because after creating, navigation opens that new tracker.
 */
internal sealed interface EditorResult {
    data class Saved(
        val trackerId: String,
    ) : EditorResult

    data object Deleted : EditorResult
}

/** Whether the tracker being edited has been looked up yet, and whether it exists. */
internal enum class EditorLoad { Loading, Ready, NotFound }

/**
 * Everything the editor screen shows. [type] and [startDate] are only editable in create mode, because a tracker
 * keeps its type and start for good. An error is shown only after the first edit ([nameEdited]), so an untouched
 * empty field does not scold the user the moment the screen opens.
 */
internal data class TrackerEditorUiState(
    val load: EditorLoad = EditorLoad.Loading,
    val isEditing: Boolean = false,
    val name: String = "",
    val nameEdited: Boolean = false,
    val type: TrackerType = TrackerType.EVENT_COUNTER,
    val startDate: LocalDate,
    val today: LocalDate,
    val isSaving: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val result: EditorResult? = null,
) {
    private val validation: TrackerNameValidation get() = validateTrackerName(name)

    /** The failure to show under the field, or null when the name is fine or still untouched. */
    val nameError: TrackerNameValidation? get() = validation.takeIf { nameEdited && it !is TrackerNameValidation.Valid }

    val canSave: Boolean get() = load == EditorLoad.Ready && validation is TrackerNameValidation.Valid && !isSaving
}
