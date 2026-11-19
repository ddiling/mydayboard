package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for trackers and events that were already deleted: they behave like unknown ids. */
abstract class TrackerDeletedIdContractTest : TrackerRepositoryContractBase() {
    @Test
    fun `renaming a deleted tracker does not bring it back`() =
        runTest {
            val tracker = givenDeletedTracker()

            repo.renameTracker(tracker.id, "Coffee")

            assertNull(repo.observeTracker(tracker.id).first())
        }

    @Test
    fun `deleting a deleted tracker again changes nothing`() =
        runTest {
            val tracker = givenDeletedTracker()

            repo.deleteTracker(tracker.id)

            assertEquals(emptyList<Any>(), repo.observeTrackers().first())
        }

    @Test
    fun `adding an event to a deleted tracker returns null`() =
        runTest {
            assertNull(repo.addEvent(givenDeletedTracker().id, now))
        }

    @Test
    fun `adding an event to a deleted tracker stores nothing`() =
        runTest {
            repo.addEvent(givenDeletedTracker().id, now)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `updating an event of a deleted tracker changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val event = givenEvent(tracker)
            repo.deleteTracker(tracker.id)

            repo.updateEventTime(event.id, newestEventTime)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `deleting an event of a deleted tracker changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val event = givenEvent(tracker)
            repo.deleteTracker(tracker.id)

            repo.deleteEvent(event.id)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `updating a deleted event does not bring it back`() =
        runTest {
            val tracker = givenTracker()
            val event = givenDeletedEvent(tracker)

            repo.updateEventTime(event.id, newestEventTime)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }

    @Test
    fun `deleting a deleted event again changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val event = givenDeletedEvent(tracker)

            repo.deleteEvent(event.id)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }
}
