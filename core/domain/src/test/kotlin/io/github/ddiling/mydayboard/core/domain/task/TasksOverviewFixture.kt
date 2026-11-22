package io.github.ddiling.mydayboard.core.domain.task

import io.github.ddiling.mydayboard.core.testing.FakeTaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskRepository
import io.github.ddiling.mydayboard.core.testing.FakeTaskStore
import io.github.ddiling.mydayboard.core.testing.FakeTaskTickRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/** Fake repositories sharing one store and a clock the test moves; the minute ticker is a flow the test emits on. */
internal class TasksOverviewFixture(
    start: Instant = warsaw(2026, 11, 18, 12),
) {
    val today: LocalDate = LocalDate.of(2026, 11, 18)
    val clock = MutableClock(start, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTaskStore()
    val tasks = FakeTaskRepository(clock, ids, store)
    val categories = FakeTaskCategoryRepository(clock, ids, store)
    val ticks = FakeTaskTickRepository(clock, ids, store)
    val ticker = MutableSharedFlow<Instant>(replay = 1)
    val useCase = ObserveTasksOverviewUseCase(tasks, categories, ticks, clock) { ticker }

    init {
        tick()
    }

    fun tick() {
        ticker.tryEmit(clock.instant())
    }

    suspend fun overview(): TasksOverview = useCase().first()

    suspend fun rowOf(task: Task): TaskRow = overview().rows.single { it.task.id == task.id }

    /** [edit] sets the less common draft fields (goal, miss rule, category) so the helper stays small. */
    suspend fun addTask(
        name: String,
        repeat: Repeat = Repeat.EveryDay,
        time: LocalTime? = null,
        startDate: LocalDate = today,
        edit: TaskDraft.() -> TaskDraft = { this },
    ): Task = tasks.createTask(TaskDraft(name, null, repeat, time, null, null, OnMiss.EXPIRES).edit(), startDate)
}
