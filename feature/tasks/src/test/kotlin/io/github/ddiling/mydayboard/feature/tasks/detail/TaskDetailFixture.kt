package io.github.ddiling.mydayboard.feature.tasks.detail

import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.FakeTaskTickRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Fake repositories sharing one store and a clock the test moves. The clock starts on Wednesday 2026-11-18
 * 12:00 in Warsaw.
 */
internal class TaskDetailFixture(
    now: Instant = warsaw(2026, 11, 18, 12),
) {
    val today: LocalDate = LocalDate.of(2026, 11, 18)
    val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    val tasks = FakeTaskRepository(clock, ids, store)
    val categories = FakeTaskCategoryRepository(clock, ids, store)
    val ticks = FakeTaskTickRepository(clock, ids, store)

    suspend fun addTask(
        name: String,
        repeat: Repeat = Repeat.EveryDay,
        goal: Int? = null,
        onMiss: OnMiss = OnMiss.EXPIRES,
        startDate: LocalDate = today,
    ): Task = tasks.createTask(TaskDraft(name, null, repeat, null, null, goal, onMiss), startDate)

    suspend fun tickOn(
        task: Task,
        day: LocalDate,
        slot: LocalTime? = null,
    ) = ticks.addTick(task.id, day, slot)

    /** What the screen would show for [task] today, from the ticks stored so far. */
    suspend fun content(task: Task): TaskDetailUiState.Content =
        detailContent(task, null, ticks.observeTicks(task.id).first(), today)
}
