package io.github.ddiling.mydayboard.core.data

import androidx.room.Room
import io.github.ddiling.mydayboard.core.database.MyDayboardDatabase
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Room-specific checks that the contract cannot express: the contract only sees the domain API, where events
 * of a deleted tracker are already hidden, so an event write that wrongly went through is invisible there.
 * Here the event row is read back through the event DAO, which does not look at the tracker.
 */
@RunWith(RobolectricTestRunner::class)
class OfflineTrackerRepositoryTest {
    private val start = Instant.parse("2025-03-01T10:00:00Z")
    private val clock = MutableClock(start, ZoneOffset.UTC)
    private val database =
        Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), MyDayboardDatabase::class.java).build()
    private val repo =
        OfflineTrackerRepository(database.trackerDao(), database.trackerEventDao(), clock, SequentialIdGenerator())

    @After
    fun closeDatabase() = database.close()

    @Test
    fun `updating an event of a deleted tracker leaves its row unchanged`() =
        runTest {
            val event = givenEventOfDeletedTracker()
            val before = database.trackerEventDao().getActiveById(event.id)

            clock.advanceBy(Duration.ofMinutes(5))
            repo.updateEventTime(event.id, start.plus(Duration.ofHours(1)))

            assertEquals(before, database.trackerEventDao().getActiveById(event.id))
        }

    @Test
    fun `deleting an event of a deleted tracker leaves its row unchanged`() =
        runTest {
            val event = givenEventOfDeletedTracker()
            val before = database.trackerEventDao().getActiveById(event.id)

            clock.advanceBy(Duration.ofMinutes(5))
            repo.deleteEvent(event.id)

            assertEquals(before, database.trackerEventDao().getActiveById(event.id))
        }

    private suspend fun givenEventOfDeletedTracker(): TrackerEvent {
        val tracker = repo.createTracker("Name", TrackerType.EVENT_COUNTER, start)
        val event = checkNotNull(repo.addEvent(tracker.id, start))
        repo.deleteTracker(tracker.id)
        return event
    }
}
