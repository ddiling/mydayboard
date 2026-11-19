package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for what deleting a tracker or an event makes invisible. */
abstract class TrackerDeletionContractTest : TrackerRepositoryContractBase() {
    @Test
    fun `deleted tracker disappears from observed trackers`() =
        runTest {
            givenDeletedTracker()

            assertEquals(emptyList<Tracker>(), repo.observeTrackers().first())
        }

    @Test
    fun `observing a tracker that gets deleted emits null`() =
        runTest {
            val tracker = givenTracker()
            repo.observeTracker(tracker.id).test {
                assertEquals(tracker, awaitItem())
                repo.deleteTracker(tracker.id)
                assertNull(awaitItem())
            }
        }

    @Test
    fun `deleting a tracker removes its events from all events`() =
        runTest {
            val tracker = givenTracker()
            givenEvent(tracker)

            repo.deleteTracker(tracker.id)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `deleting a tracker empties its own events`() =
        runTest {
            val tracker = givenTracker()
            givenEvent(tracker)

            repo.deleteTracker(tracker.id)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }

    @Test
    fun `deleting a tracker keeps the events of other trackers`() =
        runTest {
            val gone = givenTracker()
            val kept = givenTracker()
            givenEvent(gone)
            val keptEvent = givenEvent(kept)

            repo.deleteTracker(gone.id)

            assertEquals(listOf(keptEvent), repo.observeAllEvents().first())
        }

    @Test
    fun `deleted event disappears from observed events`() =
        runTest {
            val tracker = givenTracker()
            val event = givenEvent(tracker)
            repo.observeEvents(tracker.id).test {
                assertEquals(listOf(event), awaitItem())
                repo.deleteEvent(event.id)
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `deleting an event keeps the tracker's other events`() =
        runTest {
            val tracker = givenTracker()
            val gone = givenEvent(tracker, oldestEventTime)
            val kept = givenEvent(tracker, newestEventTime)

            repo.deleteEvent(gone.id)

            assertEquals(listOf(kept), repo.observeEvents(tracker.id).first())
        }
}
