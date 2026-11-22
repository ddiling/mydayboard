package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.OnMiss
import io.github.ddiling.mydayboard.core.domain.task.Repeat
import io.github.ddiling.mydayboard.core.domain.task.Task
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskDraft
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Shared fixtures of the task repository contract tests, split by topic into [TaskContractTest],
 * [TaskChangeContractTest], [TaskCategoryLinkContractTest] and [TaskCategoryContractTest]. The in-memory fake and
 * the Room implementation each extend all of them, which is what proves they behave identically. Both
 * repositories of one test must see the same data, so an implementation builds them over one store or database.
 */
abstract class TaskRepositoryContractBase {
    abstract fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository

    abstract fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository

    /** The clock's reading until a test advances it. */
    protected val now: Instant = Instant.parse("2025-03-01T10:00:00Z")
    protected val clock = MutableClock(now, ZoneOffset.UTC)
    protected val ids = SequentialIdGenerator()
    protected val tasks: TaskRepository by lazy { createTaskRepository(clock, ids) }
    protected val categories: TaskCategoryRepository by lazy { createCategoryRepository(clock, ids) }

    /** How far a test moves [clock] so that `updatedAt` can differ from `createdAt`. */
    protected val clockAdvance: Duration = Duration.ofMinutes(3)
    protected val nowAfterAdvance: Instant = now.plus(clockAdvance)

    /** Not the clock's day, so a test can tell a given start date from one taken from the clock. */
    protected val startDay: LocalDate = LocalDate.of(2025, 3, 10)

    protected val draft =
        TaskDraft("Vacuum", null, Repeat.EveryDay, null, null, null, OnMiss.STAYS)

    protected suspend fun givenTask(draft: TaskDraft = this.draft): Task = tasks.createTask(draft, startDay)

    protected suspend fun givenDeletedTask(): Task = givenTask().also { tasks.deleteTask(it.id) }

    protected suspend fun givenCategory(name: String = "Home"): TaskCategory = categories.createCategory(name)

    protected suspend fun givenDeletedCategory(): TaskCategory =
        givenCategory().also { categories.deleteCategory(it.id) }
}
