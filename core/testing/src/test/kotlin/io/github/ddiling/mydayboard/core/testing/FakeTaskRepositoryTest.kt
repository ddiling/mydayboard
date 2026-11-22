package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository

class FakeTaskCategoryContractTest : TaskCategoryContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}

class FakeTaskContractTest : TaskContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}

class FakeTaskCategoryLinkContractTest : TaskCategoryLinkContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}

class FakeTaskChangeContractTest : TaskChangeContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}
