package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.tracker.ReasonTextValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.validateReasonText
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerReasonsDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Lists, adds, edits and deletes the reasons of one tracker. A delete is the repository's soft delete, so Undo
 * is a restore that keeps the reason's old `createdAt` and therefore its old place in the list. Only the latest
 * delete can be undone: an Undo that reaches back past a newer delete would surprise the user.
 */
@HiltViewModel
internal class TrackerReasonsViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        trackers: TrackerRepository,
        private val reasons: TrackerReasonRepository,
    ) : ViewModel() {
        private val trackerId: String = savedStateHandle.toRoute<TrackerReasonsDestination>().trackerId
        private val editor = MutableStateFlow<ReasonEditor?>(null)
        private val pendingUndoId = MutableStateFlow<String?>(null)

        val uiState: StateFlow<TrackerReasonsUiState> =
            combine(
                trackers.observeTracker(trackerId),
                reasons.observeReasons(trackerId),
                editor,
                pendingUndoId,
            ) { tracker, list, editor, undoId ->
                if (tracker == null) {
                    TrackerReasonsUiState.NotFound
                } else {
                    TrackerReasonsUiState.Content(tracker.name, list, editor, undoId)
                }
            }.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
                TrackerReasonsUiState.Loading,
            )

        fun onAddClick() {
            editor.value = ReasonEditor(reasonId = null, text = "")
        }

        fun onEditClick(id: String) {
            val current = (uiState.value as? TrackerReasonsUiState.Content)?.reasons?.firstOrNull { it.id == id }
            current?.let { editor.value = ReasonEditor(it.id, it.text) }
        }

        fun onTextChange(text: String) {
            editor.value = editor.value?.copy(text = text)
        }

        /** Stores the trimmed text, or does nothing when it is blank or too long (the dialog shows why). */
        fun onSave() {
            val open = editor.value ?: return
            val valid = validateReasonText(open.text) as? ReasonTextValidation.Valid ?: return
            editor.value = null
            viewModelScope.launch {
                if (open.reasonId == null) {
                    reasons.addReason(trackerId, valid.text)
                } else {
                    reasons.updateReason(open.reasonId, valid.text)
                }
            }
        }

        fun onDismissEditor() {
            editor.value = null
        }

        fun onSwipeDelete(id: String) {
            viewModelScope.launch {
                reasons.deleteReason(id)
                pendingUndoId.value = id
            }
        }

        fun onUndoDelete() {
            val id = pendingUndoId.getAndUpdate { null } ?: return
            viewModelScope.launch { reasons.restoreReason(id) }
        }

        fun onUndoDismissed() {
            pendingUndoId.value = null
        }

        private companion object {
            const val STOP_TIMEOUT_MILLIS = 5_000L
        }
    }
