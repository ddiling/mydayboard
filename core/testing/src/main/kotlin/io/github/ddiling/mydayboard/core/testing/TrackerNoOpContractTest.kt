package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for operations on unknown or deleted ids: they change nothing and do not throw. */
abstract class TrackerNoOpContractTest : TrackerRepositoryContractBase() {
    @Test
    fun renamingOrDeletingUnknownTrackerChangesNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)

            repo.renameTracker("nope", "X")
            repo.deleteTracker("nope")

            assertEquals(listOf(tracker), repo.observeTrackers().first())
        }

    @Test
    fun renamingOrDeletingDeletedTrackerChangesNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            repo.renameTracker(tracker.id, "X")
            repo.deleteTracker(tracker.id)

            assertEquals(emptyList<Any>(), repo.observeTrackers().first())
            assertNull(repo.observeTracker(tracker.id).first())
        }

    @Test
    fun addingEventToUnknownTrackerReturnsNullAndStoresNothing() =
        runTest {
            assertNull(repo.addEvent("nope", start))
            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun addingEventToDeletedTrackerReturnsNullAndStoresNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            assertNull(repo.addEvent(tracker.id, start))
            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun updatingOrDeletingUnknownEventChangesNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, minute1)!!

            repo.updateEventTime("nope", minute9)
            repo.deleteEvent("nope")

            assertEquals(listOf(event), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun updatingOrDeletingEventOfDeletedTrackerChangesNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, minute1)!!
            repo.deleteTracker(tracker.id)

            repo.updateEventTime(event.id, minute9)
            repo.deleteEvent(event.id)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun updatingOrDeletingDeletedEventChangesNothing() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, minute1)!!
            repo.deleteEvent(event.id)

            repo.updateEventTime(event.id, minute9)
            repo.deleteEvent(event.id)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }
}
