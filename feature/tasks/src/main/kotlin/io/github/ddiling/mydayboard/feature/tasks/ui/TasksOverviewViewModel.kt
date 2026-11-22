package io.github.ddiling.mydayboard.feature.tasks.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.task.ObserveTasksOverviewUseCase
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.domain.task.TickOutcome
import io.github.ddiling.mydayboard.core.domain.task.TasksOverview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

private const val SUBSCRIPTION_TIMEOUT_MS = 5_000L

/** Shared by the dashboard card and the list screen. */
@HiltViewModel
internal class TasksOverviewViewModel
    @Inject
    constructor(
        observe: ObserveTasksOverviewUseCase,
        private val taskActions: TaskActions,
    ) : ViewModel() {
        val overview: StateFlow<TasksOverview?> =
            observe().stateIn(viewModelScope, SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MS), null)

        suspend fun swipeDone(id: String): TickOutcome.Added? = taskActions.swipeDone(id)

        suspend fun undo(outcome: TickOutcome) = taskActions.undo(outcome)
    }
