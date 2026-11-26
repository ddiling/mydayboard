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
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryDialogActions
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryDialogState
import io.github.ddiling.mydayboard.feature.tasks.categories.CategoryEditor
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskFormDestination
import io.github.ddiling.mydayboard.feature.tasks.navigation.TasksListNotice
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
 * Archive and delete act on the stored task, not on unsaved edits, and report back as a [TasksListNotice] so the
 * list can say what happened. The field changes live in [actions]; each is a pure reducer on [TaskFormUiState].
 */
@HiltViewModel
internal class TaskFormViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val tasks: TaskRepository,
        categories: TaskCategoryRepository,
        private val categoryEditor: CategoryEditor,
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
            categories
                .observeCategories()
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), emptyList())

        /** The category dialog, opened by the "New category" row. */
        val categoryDialog: StateFlow<CategoryDialogState> = categoryEditor.state

        val categoryDialogActions =
            CategoryDialogActions(
                onNameChange = { name -> viewModelScope.launch { categoryEditor.onNameChange(name) } },
                onSave = ::onCategorySave,
                onCancel = categoryEditor::dismiss,
                onDelete = { viewModelScope.launch { categoryEditor.onDelete() } },
            )

        val actions =
            TaskFormActions(
                onNameChange = { edit { withName(it) } },
                onCategoryChange = { edit { withCategory(it) } },
                onNewCategory = { viewModelScope.launch { categoryEditor.open(null) } },
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
                onSave = ::onSave,
                onSavedHandled = ::onSavedHandled,
                onArchive = ::onArchive,
                onDeleteRequest = ::onDeleteRequest,
                onDeleteConfirm = ::onDeleteConfirm,
                onDeleteDismiss = ::onDeleteDismiss,
                onFinishedHandled = ::onFinishedHandled,
            )

        private var saveStarted = false
        private var finishStarted = false
        private var storedName: String? = null

        init {
            if (taskId != null) loadTask(taskId)
        }

        private fun edit(change: TaskFormUiState.() -> TaskFormUiState) = _uiState.update(change)

        /** The task is read once: later changes from elsewhere must not overwrite what the user is typing. */
        private fun loadTask(id: String) {
            viewModelScope.launch {
                val task = tasks.observeTask(id).first()
                storedName = task?.name
                _uiState.update {
                    task?.toFormState(it.today) ?: it.copy(load = FormLoad.NotFound).revalidated()
                }
            }
        }

        /** The category made in the dialog is chosen at once, so the user does not have to find it in the menu. */
        private fun onCategorySave() {
            viewModelScope.launch { categoryEditor.onSave()?.let { id -> edit { withCategory(id) } } }
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

        fun onSavedHandled() = _uiState.update { it.copy(saved = false) }

        /** Archives at once, without a question: it can be undone from the list. */
        fun onArchive() {
            val name = storedName
            val id = taskId
            if (name == null || id == null || finishStarted) return
            finishStarted = true
            viewModelScope.launch {
                tasks.archiveTask(id)
                _uiState.update { it.copy(finished = TasksListNotice.Archived(id, name)) }
            }
        }

        /** Only opens the question; nothing is changed until [onDeleteConfirm]. */
        fun onDeleteRequest() {
            val name = storedName ?: return
            _uiState.update { it.copy(deleteDialogName = name) }
        }

        fun onDeleteDismiss() = _uiState.update { it.copy(deleteDialogName = null) }

        fun onDeleteConfirm() {
            val name = _uiState.value.deleteDialogName
            val id = taskId
            if (name == null || id == null || finishStarted) return
            finishStarted = true
            viewModelScope.launch {
                tasks.deleteTask(id)
                _uiState.update { it.copy(deleteDialogName = null, finished = TasksListNotice.Deleted(name)) }
            }
        }

        fun onFinishedHandled() = _uiState.update { it.copy(finished = null) }
    }
