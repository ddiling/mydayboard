package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.feature.tasks.common.UndoOffer
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskDetailDestination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalTime
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/**
 * One task's details. The message with Undo holds only the latest tick or untick; a newer one replaces it and
 * the older change stays.
 */
@HiltViewModel
internal class TaskDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        observe: ObserveTaskDetail,
        private val actions: TaskActions,
    ) : ViewModel() {
        private val taskId: String = savedStateHandle.toRoute<TaskDetailDestination>().taskId
        private val undo = MutableStateFlow<DetailUndo?>(null)

        val uiState: StateFlow<TaskDetailUiState> =
            observe(taskId)
                .combine(undo) { state, undo ->
                    if (state is TaskDetailUiState.Content) state.copy(pendingUndo = undo) else state
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS),
                    TaskDetailUiState.Loading,
                )

        /** Ticks, adds one, or unticks today, as the button says. Does nothing for [BigButton.None]. */
        fun onBigButton() {
            val content = uiState.value as? TaskDetailUiState.Content ?: return
            val name = content.task.name
            viewModelScope.launch {
                undo.value =
                    when (content.bigButton) {
                        BigButton.MarkDone, BigButton.PlusOne, is BigButton.TickNext -> {
                            actions.swipeDone(taskId)?.let { DetailUndo.Ticked(UndoOffer(name, it)) }
                        }

                        BigButton.UntickToday -> {
                            actions.untickToday(taskId)?.let { DetailUndo.Unticked(name, it) }
                        }

                        BigButton.None -> {
                            null
                        }
                    } ?: undo.value
            }
        }

        /** Ticks [time] when it is open and unticks it when it is done. */
        fun onChipClick(time: LocalTime) {
            val content = uiState.value as? TaskDetailUiState.Content ?: return
            val chip = content.chips.firstOrNull { it.time == time } ?: return
            val name = content.task.name
            viewModelScope.launch {
                if (chip.ticked) {
                    actions.untickSlotToday(taskId, time)?.let { undo.value = DetailUndo.Unticked(name, it) }
                } else {
                    actions.tickSlotToday(taskId, time)?.let { undo.value = DetailUndo.Ticked(UndoOffer(name, it)) }
                }
            }
        }

        fun onUndo() {
            val pending = undo.getAndUpdate { null } ?: return
            viewModelScope.launch { actions.undo(pending.outcome) }
        }

        fun onUndoDismissed() = undo.update { null }
    }
