package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerNameValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.domain.tracker.validateTrackerName
import io.github.ddiling.mydayboard.feature.trackers.navigation.TrackerEditorDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject

/**
 * Creates, renames and deletes one tracker. The repository does not validate, so the name is checked here and
 * the normalized text is what gets stored. A write sets [TrackerEditorUiState.isSaving] before it starts, so a
 * double tap cannot create two trackers.
 */
@HiltViewModel
internal class TrackerEditorViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val repository: TrackerRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val trackerId: String? = savedStateHandle.toRoute<TrackerEditorDestination>().trackerId
        private val today = LocalDate.now(clock)
        private val _uiState =
            MutableStateFlow(
                TrackerEditorUiState(
                    load = if (trackerId == null) EditorLoad.Ready else EditorLoad.Loading,
                    isEditing = trackerId != null,
                    startDate = today,
                    today = today,
                ),
            )
        val uiState: StateFlow<TrackerEditorUiState> = _uiState.asStateFlow()

        init {
            if (trackerId != null) loadTracker(trackerId)
        }

        private fun loadTracker(id: String) {
            viewModelScope.launch {
                val tracker = repository.observeTracker(id).first()
                _uiState.update {
                    if (tracker == null) {
                        it.copy(load = EditorLoad.NotFound)
                    } else {
                        it.copy(load = EditorLoad.Ready, name = tracker.name)
                    }
                }
            }
        }

        fun onNameChange(name: String) = _uiState.update { it.copy(name = name, nameEdited = true) }

        fun onTypeChange(type: TrackerType) = _uiState.update { it.copy(type = type) }

        /** A date after today is ignored: a tracker cannot have started in the future. */
        fun onStartDateChange(date: LocalDate) =
            _uiState.update { if (date.isAfter(it.today)) it else it.copy(startDate = date) }

        fun onSave() {
            val state = _uiState.value
            val valid = validateTrackerName(state.name) as? TrackerNameValidation.Valid
            if (!state.canSave || valid == null) return
            _uiState.update { it.copy(isSaving = true) }
            viewModelScope.launch {
                val savedId =
                    if (trackerId == null) {
                        repository.createTracker(valid.name, state.type, state.startedAt()).id
                    } else {
                        repository.renameTracker(trackerId, valid.name)
                        trackerId
                    }
                _uiState.update { it.copy(result = EditorResult.Saved(savedId)) }
            }
        }

        fun onDeleteClick() = _uiState.update { it.copy(showDeleteConfirmation = true) }

        fun onDeleteDismiss() = _uiState.update { it.copy(showDeleteConfirmation = false) }

        fun onDeleteConfirm() {
            val id = trackerId ?: return
            if (_uiState.value.isSaving) return
            _uiState.update { it.copy(showDeleteConfirmation = false, isSaving = true) }
            viewModelScope.launch {
                repository.deleteTracker(id)
                _uiState.update { it.copy(result = EditorResult.Deleted) }
            }
        }

        /**
         * Today keeps the current time of day, so a tracker started now does not begin in the past; any earlier
         * day begins at its midnight in the current zone (a DST day may not be 24 hours long, so the zone, not a
         * fixed offset, decides).
         */
        private fun TrackerEditorUiState.startedAt(): Instant =
            if (type == TrackerType.DAYS_SINCE && startDate != today) {
                startDate.atStartOfDay(clock.zone).toInstant()
            } else {
                clock.instant()
            }
    }
