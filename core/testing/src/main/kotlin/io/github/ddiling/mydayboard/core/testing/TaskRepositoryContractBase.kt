package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategory
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Shared fixtures of the task repository contract tests, split by topic into [TaskContractTest],
 * [TaskChangeContractTest], [TaskCategoryLinkContractTest] and [TaskCategoryContractTest]. The in-memory fake and
 * the Room implementation each extend all of them, which is what proves they behave identically. Both
 * repositories of one test must see the same data, so an implementation builds them over one store or database.
 */
abstract class TaskRepositoryContractBase {
    abstract fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository

    /** The clock's reading until a test advances it. */
    protected val now: Instant = Instant.parse("2025-03-01T10:00:00Z")
    protected val clock = MutableClock(now, ZoneOffset.UTC)
    protected val ids = SequentialIdGenerator()
    protected val categories: TaskCategoryRepository by lazy { createCategoryRepository(clock, ids) }

    /** How far a test moves [clock] so that `updatedAt` can differ from `createdAt`. */
    protected val clockAdvance: Duration = Duration.ofMinutes(3)
    protected val nowAfterAdvance: Instant = now.plus(clockAdvance)

    protected suspend fun givenCategory(name: String = "Home"): TaskCategory = categories.createCategory(name)

    protected suspend fun givenDeletedCategory(): TaskCategory =
        givenCategory().also { categories.deleteCategory(it.id) }
}
