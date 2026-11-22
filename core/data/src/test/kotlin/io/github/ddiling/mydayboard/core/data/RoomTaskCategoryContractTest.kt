package io.github.ddiling.mydayboard.core.data

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.task.TaskCategoryRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TaskCategoryContractTest
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

    fun categories(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = with(database.value) { OfflineTaskCategoryRepository(taskCategoryDao(), clock, ids) }

    fun close() {
        if (database.isInitialized()) database.value.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class RoomTaskCategoryContractTest : TaskCategoryContractTest() {
    private val fixture = RoomTaskFixture()

    override fun createCategoryRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TaskCategoryRepository = fixture.categories(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}
