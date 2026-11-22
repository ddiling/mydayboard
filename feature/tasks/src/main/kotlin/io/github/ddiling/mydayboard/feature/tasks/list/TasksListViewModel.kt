package io.github.ddiling.mydayboard.feature.tasks.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.task.ObserveTasksOverviewUseCase
import io.github.ddiling.mydayboard.core.domain.task.RowState
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.domain.task.TasksOverview
import io.github.ddiling.mydayboard.core.domain.task.groupedByCategory
import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * The full task list. Rows come from the shared overview, so a day change or a tick made elsewhere shows up
 * here without a refresh. A swipe is one step; the message that offers Undo holds only the latest swipe, so a
 * newer swipe replaces an older message (the older tick stays).
 */
@HiltViewModel
internal class TasksListViewModel
    @Inject
    constructor(
        overview: ObserveTasksOverviewUseCase,
        private val actions: TaskActions,
    ) : ViewModel() {
        private val undo = MutableStateFlow<UndoOffer?>(null)

        val uiState: StateFlow<TasksListUiState> =
            combine(overview(), undo) { overview, undo -> overview.toUiState(undo) }
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                    TasksListUiState.Loading,
                )

        /** Ticks the task, late ones included. Does nothing when the task is gone or has nothing left to tick. */
        fun onSwipeDone(taskId: String) {
            val name = (uiState.value as? TasksListUiState.Content)?.nameOf(taskId) ?: return
            viewModelScope.launch {
                actions.swipeDone(taskId)?.let { outcome -> undo.value = UndoOffer(name, outcome) }
            }
        }

        fun onUndo() {
            val offer = undo.getAndUpdate { null } ?: return
            viewModelScope.launch { actions.undo(offer.outcome) }
        }

        /** Clears [offer] only; a newer swipe that replaced it has its own message, which stays. */
        fun onUndoDismissed(offer: UndoOffer) {
            undo.compareAndSet(offer, null)
        }
    }

private fun TasksOverview.toUiState(undo: UndoOffer?): TasksListUiState =
    if (rows.isEmpty()) {
        TasksListUiState.Empty
    } else {
        TasksListUiState.Content(
            groups = groupedByCategory(),
            today = today,
            lateCount = rows.count { it.state == RowState.LATE },
            archivedCount = archivedCount,
            undo = undo,
        )
    }

private fun TasksListUiState.Content.nameOf(taskId: String): String? =
    groups.firstNotNullOfOrNull { group -> group.rows.firstOrNull { it.task.id == taskId } }?.task?.name
