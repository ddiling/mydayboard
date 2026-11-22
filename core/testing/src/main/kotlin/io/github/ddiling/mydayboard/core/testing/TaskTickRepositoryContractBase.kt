package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskTick
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.LocalTime

/** Fixtures of [TaskTickContractTest], [TaskTickObserveContractTest] and [TaskTickDayContractTest]. */
abstract class TaskTickRepositoryContractBase : TaskRepositoryContractBase() {
    abstract fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository

    protected val ticks: TaskTickRepository by lazy { createTickRepository(clock, ids) }

    protected val day: LocalDate = LocalDate.of(2025, 3, 12)
    protected val otherDay: LocalDate = day.plusDays(1)
    protected val morning: LocalTime = LocalTime.of(8, 0)
    protected val evening: LocalTime = LocalTime.of(20, 0)

    protected suspend fun givenTick(
        taskId: String,
        tickDay: LocalDate = day,
        slot: LocalTime? = null,
    ): TaskTick = checkNotNull(ticks.addTick(taskId, tickDay, slot)) { "the task should be live" }

    protected suspend fun liveTicksOf(taskId: String): List<TaskTick> = ticks.observeTicks(taskId).first()
}
