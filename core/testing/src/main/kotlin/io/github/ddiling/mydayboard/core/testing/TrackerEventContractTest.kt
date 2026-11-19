package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for adding, ordering, moving and deleting events. */
abstract class TrackerEventContractTest : TrackerRepositoryContractBase() {
    @Test
    fun eventsAreOrderedByOccurrenceTimeDescending() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val middle = repo.addEvent(tracker.id, minute5)!!
            val newest = repo.addEvent(tracker.id, minute9)!!
            val oldest = repo.addEvent(tracker.id, minute1)!!

            assertEquals(listOf(newest, middle, oldest), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun addedEventBelongsToTrackerAndTakesClockTimestamps() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(step)

            val event = repo.addEvent(tracker.id, earlier)!!

            assertEquals(tracker.id, event.trackerId)
            assertEquals(earlier, event.occurredAt)
            assertEquals(afterStep, event.createdAt)
            assertEquals(afterStep, event.updatedAt)
            assertEquals(listOf(event), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun updatingEventTimeReSortsAndBumpsUpdatedAt() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val older = repo.addEvent(tracker.id, minute1)!!
            val newer = repo.addEvent(tracker.id, minute5)!!
            clock.advanceBy(step)

            repo.updateEventTime(older.id, minute8)

            val moved = older.copy(occurredAt = minute8, updatedAt = afterStep)
            assertEquals(listOf(moved, newer), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun deletedEventDisappears() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, minute1)!!
            repo.observeEvents(tracker.id).test {
                assertEquals(listOf(event), awaitItem())
                repo.deleteEvent(event.id)
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun allEventsFlowEmitsAgainAfterEachChange() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.observeAllEvents().test {
                assertTrue(awaitItem().isEmpty())
                val event = repo.addEvent(tracker.id, minute1)!!
                assertEquals(listOf(event), awaitItem())
                repo.updateEventTime(event.id, minute2)
                assertEquals(minute2, awaitItem().single().occurredAt)
                repo.deleteEvent(event.id)
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun eventsFlowDoesNotEmitForOtherTrackersEvents() =
        runTest {
            val observed = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val other = repo.createTracker("B", TrackerType.EVENT_COUNTER, start)
            repo.observeEvents(observed.id).test {
                assertTrue(awaitItem().isEmpty())

                repo.addEvent(other.id, minute1)

                expectNoEvents()
            }
        }
}
