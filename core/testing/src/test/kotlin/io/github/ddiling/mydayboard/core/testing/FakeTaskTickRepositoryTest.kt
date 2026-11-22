package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository

/** One store shared by the three fakes of a test, like the one database behind the Room ones. */
private class FakeTaskTickFixture {
    private val store = FakeTaskStore()

    fun tasks(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = FakeTaskRepository(clock, ids, store)

    fun categories(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = FakeTaskCategoryRepository(clock, ids, store)

    fun ticks(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = FakeTaskTickRepository(clock, ids, store)
}

class FakeTaskTickContractTest : TaskTickContractTest() {
    private val fixture = FakeTaskTickFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    override fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = fixture.ticks(clock, ids)
}

class FakeTaskTickObserveContractTest : TaskTickObserveContractTest() {
    private val fixture = FakeTaskTickFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    override fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = fixture.ticks(clock, ids)
}

class FakeTaskTickDayContractTest : TaskTickDayContractTest() {
    private val fixture = FakeTaskTickFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    override fun createTickRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = fixture.ticks(clock, ids)
}
