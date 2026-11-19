package io.github.ddiling.mydayboard.core.data

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TrackerContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerDeletedIdContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerDeletionContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerEventContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerUnknownIdContractTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Each Room test class extends one contract class and shares this setup; the five together prove the Room
 * repository behaves like the fake. A fresh in-memory database per test, closed afterwards.
 */
private class RoomFixture {
    private var database: MyDayboardDatabase? = null

    fun repository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository {
        val db =
            Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MyDayboardDatabase::class.java).build()
        database = db
        return OfflineTrackerRepository(db.trackerDao(), db.trackerEventDao(), clock, ids)
    }

    fun close() {
        database?.close()
    }
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerContractTest : TrackerContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerEventContractTest : TrackerEventContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerDeletionContractTest : TrackerDeletionContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerUnknownIdContractTest : TrackerUnknownIdContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerDeletedIdContractTest : TrackerDeletedIdContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}
