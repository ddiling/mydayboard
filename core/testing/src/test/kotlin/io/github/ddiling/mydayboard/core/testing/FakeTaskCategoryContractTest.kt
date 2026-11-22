package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository

class FakeTaskCategoryContractTest : TaskCategoryContractTest() {
    private val store = FakeTaskStore()

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}

class FakeTaskCategoryNoOpContractTest : TaskCategoryNoOpContractTest() {
    private val store = FakeTaskStore()

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)
}
