package io.github.ddiling.mydayboard.feature.tasks.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.STATS_WINDOW_DAYS
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.domain.task.computeTaskStats
import io.github.ddiling.mydayboard.core.domain.task.nextDueDate
import io.github.ddiling.mydayboard.core.domain.time.today
import io.github.ddiling.mydayboard.feature.tasks.navigation.TaskDetailDestination
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

internal data class TaskDetailState(
    val name: String,
    val repeat: String,
    val nextDue: String,
    val streak: Int,
    val doneOfPlanned: String,
    val windowDays: Int,
)

@HiltViewModel
internal class TaskDetailViewModel
    @Inject
    constructor(
        savedStateHandle: SavedStateHandle,
        private val tasks: TaskRepository,
        ticks: TaskTickRepository,
        private val actions: TaskActions,
        private val clock: Clock,
    ) : ViewModel() {
        private val taskId = savedStateHandle.toRoute<TaskDetailDestination>().taskId

        val state: StateFlow<TaskDetailState?> =
            combine(tasks.observeTask(taskId), ticks.observeTicks(taskId)) { task, tickList ->
                task?.let {
                    val today = clock.today()
                    val stats = computeTaskStats(it, tickList, today)
                    TaskDetailState(
                        name = it.name,
                        repeat = summary(it.repeat),
                        nextDue = nextDueDate(it, tickList, today)?.toString() ?: "-",
                        streak = stats.streak,
                        doneOfPlanned = "%.1f".format(stats.doneOfPlanned),
                        windowDays = stats.plannedDays.coerceAtMost(STATS_WINDOW_DAYS),
                    )
                }
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), null)

        fun markDone() {
            viewModelScope.launch { actions.swipeDone(taskId) }
        }

        fun untick() {
            viewModelScope.launch { actions.untickToday(taskId) }
        }

        fun archive(onDone: () -> Unit) {
            viewModelScope.launch {
                tasks.archiveTask(taskId)
                onDone()
            }
        }

        private fun summary(repeat: Repeat): String =
            when (repeat) {
                is Repeat.TimesPerDay -> "Several times a day: ${repeat.times.joinToString()}"
                Repeat.EveryDay -> "Every day"
                is Repeat.Weekdays -> "Weekdays: ${repeat.days.sorted().joinToString { it.name.take(3) }}"
                is Repeat.EveryNDays -> "Every ${repeat.days} days (${repeat.mode})"
                is Repeat.Monthly -> "Monthly, day ${repeat.dayOfMonth}"
                is Repeat.OneTime -> "One time, ${repeat.due}"
            }
    }
