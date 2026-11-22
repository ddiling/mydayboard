package io.github.ddiling.mydayboard.feature.tasks.form

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.time.today
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskFormDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * Creates or edits one task. The repository does not validate, so Save runs `validateTaskDraft` first (through
 * [TaskFormUiState.canSave]). The start date is read from the clock when Save is pressed, not when the form was
 * opened, so a form left open past midnight (or across a time zone change) still counts from the right day. The
 * field changes live in [actions]; each is a pure reducer on [TaskFormUiState].
 */
@HiltViewModel
internal class TaskFormViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val tasks: TaskRepository,
        private val categoryRepository: TaskCategoryRepository,
        private val clock: Clock,
    ) : ViewModel() {
        private val taskId: String? = savedStateHandle.toRoute<TaskFormDestination>().taskId
        private val _uiState =
            MutableStateFlow(
                TaskFormUiState(
                    today = clock.today(),
                    load = if (taskId == null) FormLoad.Ready else FormLoad.Loading,
                    isEditing = taskId != null,
                ).revalidated(),
            )
        val uiState: StateFlow<TaskFormUiState> = _uiState.asStateFlow()

        /** The categories to pick from, oldest first. */
        val categories: StateFlow<List<TaskCategory>> =
            categoryRepository
                .observeCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

        val actions =
            TaskFormActions(
                onNameChange = { edit { withName(it) } },
                onCategoryChange = { edit { withCategory(it) } },
                onRepeatTypeChange = { edit { withRepeatType(it) } },
                onAddTime = { edit { withTimeAdded(it) } },
                onRemoveTime = { edit { withTimeRemoved(it) } },
                onToggleWeekday = { edit { withWeekdayToggled(it) } },
                onIntervalChange = { edit { withInterval(it) } },
                onIntervalModeChange = { edit { withIntervalMode(it) } },
                onFirstDueChange = { edit { withFirstDue(it) } },
                onDayOfMonthChange = { edit { withDayOfMonth(it) } },
                onDueDateChange = { edit { withDueDate(it) } },
                onTimeChange = { edit { withTime(it) } },
                onNoteChange = { edit { withNote(it) } },
                onGoalOnChange = { edit { withGoalOn(it) } },
                onGoalChange = { edit { withGoal(it) } },
                onOnMissChange = { edit { withOnMiss(it) } },
                onCreateCategory = ::onCreateCategory,
                onSave = ::onSave,
                onSavedHandled = ::onSavedHandled,
            )

        private var saveStarted = false

        init {
            if (taskId != null) loadTask(taskId)
        }

        private fun edit(change: TaskFormUiState.() -> TaskFormUiState) = _uiState.update(change)

        /** The task is read once: later changes from elsewhere must not overwrite what the user is typing. */
        private fun loadTask(id: String) {
            viewModelScope.launch {
                val task = tasks.observeTask(id).first()
                _uiState.update {
                    task?.toFormState(it.today) ?: it.copy(load = FormLoad.NotFound).revalidated()
                }
            }
        }

        fun onSave() {
            val state = _uiState.value
            if (!state.canSave || saveStarted) return
            saveStarted = true
            val draft = state.toDraft()
            val startDate = state.startDate(clock.today())
            viewModelScope.launch {
                if (taskId == null) tasks.createTask(draft, startDate) else tasks.updateTask(taskId, draft)
                _uiState.update { it.copy(saved = true).revalidated() }
            }
        }

        private fun onCreateCategory(name: String) {
            val trimmed = name.trim()
            if (trimmed.isEmpty()) return
            viewModelScope.launch {
                val created = categoryRepository.createCategory(trimmed)
                edit { withCategory(created.id) }
            }
        }

        fun onSavedHandled() = _uiState.update { it.copy(saved = false) }
    }
