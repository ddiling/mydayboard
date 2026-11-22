package io.github.ddiling.mydayboard.core.data

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskRepository
import io.github.ddiling.mydayboard.core.domain.task.TaskTickRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TaskCategoryContractTest
import io.github.ddiling.mydayboard.core.testing.TaskCategoryLinkContractTest
import io.github.ddiling.mydayboard.core.testing.TaskCategoryNoOpContractTest
import io.github.ddiling.mydayboard.core.testing.TaskChangeContractTest
import io.github.ddiling.mydayboard.core.testing.TaskContractTest
import io.github.ddiling.mydayboard.core.testing.TaskTickContractTest
import io.github.ddiling.mydayboard.core.testing.TaskTickDayContractTest
import io.github.ddiling.mydayboard.core.testing.TaskTickObserveContractTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Shared setup of the Room task contract classes: one in-memory database per test, shared by both repositories. */
private class RoomTaskFixture {
    private val database =
        lazy {
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    MyDayboardDatabase::class.java,
                ).build()
        }

    fun tasks(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = with(database.value) { OfflineTaskRepository(taskDao(), taskCategoryDao(), clock, ids) }

    fun categories(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = with(database.value) { OfflineTaskCategoryRepository(taskCategoryDao(), clock, ids) }

    fun ticks(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskTickRepository = OfflineTaskTickRepository(database.value.taskTickDao(), clock, ids)

    fun close() {
        if (database.isInitialized()) database.value.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskContractTest : TaskContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskChangeContractTest : TaskChangeContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskCategoryLinkContractTest : TaskCategoryLinkContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskCategoryContractTest : TaskCategoryContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskCategoryNoOpContractTest : TaskCategoryNoOpContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createTaskRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskRepository = fixture.tasks(clock, ids)

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskTickContractTest : TaskTickContractTest() {
    private val fixture = RoomTaskFixture()

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

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskTickObserveContractTest : TaskTickObserveContractTest() {
    private val fixture = RoomTaskFixture()

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

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskTickDayContractTest : TaskTickDayContractTest() {
    private val fixture = RoomTaskFixture()

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

    @After
    fun closeDatabase() = fixture.close()
}
