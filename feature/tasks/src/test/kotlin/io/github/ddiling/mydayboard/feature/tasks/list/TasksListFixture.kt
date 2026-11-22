package io.github.ddiling.mydayboard.feature.tasks.list

import io.github.ddiling.mydayboard.core.domain.task.ObserveTasksOverviewUseCase
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskActions
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.FakeTaskTickRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import java.time.Instant
import java.time.LocalDate

/** Fake repositories sharing one store and a clock the test moves. The clock starts on 2026-11-18 12:00 in Warsaw. */
internal class TasksListFixture(
    now: Instant = warsaw(2026, 11, 18, 12),
) {
    val today: LocalDate = LocalDate.of(2026, 11, 18)
    val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    val tasks = FakeTaskRepository(clock, ids, store)
    val categories = FakeTaskCategoryRepository(clock, ids, store)
    val ticks = FakeTaskTickRepository(clock, ids, store)

    // Built after the Main dispatcher is set: the view model starts its scope on it.
    fun viewModel() =
        TasksListViewModel(
            ObserveTasksOverviewUseCase(tasks, categories, ticks, clock),
            TaskActions(tasks, ticks, clock),
        )

    suspend fun addTask(
        name: String,
        repeat: Repeat = Repeat.EveryDay,
        categoryId: String? = null,
        goal: Int? = null,
        onMiss: OnMiss = OnMiss.EXPIRES,
    ): Task = tasks.createTask(TaskDraft(name, categoryId, repeat, null, null, goal, onMiss), today)
}
