package io.github.ddiling.mydayboard.core.data

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TrackerContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerDeletedIdContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerDeletionContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerEventContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerReasonChangeContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerReasonContractTest
import io.github.ddiling.mydayboard.core.testing.TrackerUnknownIdContractTest
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/**
 * Each Room test class extends one contract class and shares this setup; the contract classes together prove the Room
 * repository behaves like the fake. A fresh in-memory database per test, closed afterwards.
 */
private class RoomFixture {
    // Lazy and shared, so the tracker and the reason repository of one test see the same database.
    private val database =
        lazy {
            Room
                .inMemoryDatabaseBuilder(
                    RuntimeEnvironment.getApplication(),
                    MyDayboardDatabase::class.java,
                ).build()
        }

    fun repository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository =
        with(database.value) { OfflineTrackerRepository(trackerDao(), trackerEventDao(), clock, ids) }

    fun reasonRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerReasonRepository =
        with(database.value) { OfflineTrackerReasonRepository(trackerDao(), trackerReasonDao(), clock, ids) }

    fun close() {
        if (database.isInitialized()) database.value.close()
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

@RunWith(RobolectricTestRunner::class)
class RoomTrackerReasonContractTest : TrackerReasonContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    override fun createReasonRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerReasonRepository = fixture.reasonRepository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}

@RunWith(RobolectricTestRunner::class)
class RoomTrackerReasonChangeContractTest : TrackerReasonChangeContractTest() {
    private val fixture = RoomFixture()

    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fixture.repository(clock, ids)

    override fun createReasonRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerReasonRepository = fixture.reasonRepository(clock, ids)

    @After
    fun closeDatabase() = fixture.close()
}
