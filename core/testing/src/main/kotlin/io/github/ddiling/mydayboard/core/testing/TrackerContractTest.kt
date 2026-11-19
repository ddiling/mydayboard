package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for creating, ordering, renaming and deleting trackers. */
abstract class TrackerContractTest : TrackerRepositoryContractBase() {
    @Test
    fun createdTrackerHasGeneratedIdAndClockTimestamps() =
        runTest {
            val tracker = repo.createTracker("Water", TrackerType.EVENT_COUNTER, earlier)

            assertEquals("id-1", tracker.id)
            assertEquals("Water", tracker.name)
            assertEquals(earlier, tracker.startedAt)
            assertEquals(start, tracker.createdAt)
            assertEquals(start, tracker.updatedAt)
        }

    @Test
    fun createdTrackerAppearsInObservedTrackers() =
        runTest {
            repo.observeTrackers().test {
                assertEquals(emptyList<Any>(), awaitItem())
                val tracker = repo.createTracker("Water", TrackerType.EVENT_COUNTER, start)
                assertEquals(listOf(tracker), awaitItem())
            }
        }

    @Test
    fun trackersAreOrderedByCreationTimeAscending() =
        runTest {
            val first = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(step)
            val second = repo.createTracker("B", TrackerType.DAYS_SINCE, start)

            assertEquals(listOf(first, second), repo.observeTrackers().first())
        }

    @Test
    fun renamingBumpsUpdatedAtAndKeepsCreatedAt() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(step)

            repo.renameTracker(tracker.id, "B")

            val renamed = repo.observeTracker(tracker.id).first()!!
            assertEquals("B", renamed.name)
            assertEquals(start, renamed.createdAt)
            assertEquals(afterStep, renamed.updatedAt)
        }

    @Test
    fun deletedTrackerDisappearsFromTrackersList() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            assertEquals(emptyList<Any>(), repo.observeTrackers().first())
        }

    @Test
    fun observingDeletedTrackerEmitsNull() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.observeTracker(tracker.id).test {
                assertEquals(tracker, awaitItem())
                repo.deleteTracker(tracker.id)
                assertNull(awaitItem())
            }
        }

    @Test
    fun observingMissingTrackerEmitsNull() =
        runTest {
            assertNull(repo.observeTracker("nope").first())
        }

    @Test
    fun deletingTrackerHidesItsEventsButKeepsOthers() =
        runTest {
            val gone = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val kept = repo.createTracker("B", TrackerType.EVENT_COUNTER, start)
            repo.addEvent(gone.id, minute1)
            val keptEvent = repo.addEvent(kept.id, minute2)!!

            repo.deleteTracker(gone.id)

            assertEquals(listOf(keptEvent), repo.observeAllEvents().first())
            assertTrue(repo.observeEvents(gone.id).first().isEmpty())
        }

    @Test
    fun eventsOfMissingTrackerAreEmpty() =
        runTest {
            assertTrue(repo.observeEvents("nope").first().isEmpty())
        }
}
