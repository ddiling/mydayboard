package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository

class FakeTaskTickContractTest : TaskTickContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)

    override fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = FakeTaskTickRepository(clock, ids, store)
}

class FakeTaskTickObserveContractTest : TaskTickObserveContractTest() {
    private val store = FakeTaskStore()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)

    override fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = FakeTaskTickRepository(clock, ids, store)
}
