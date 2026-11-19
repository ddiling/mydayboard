package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for adding, ordering and moving events. */
abstract class TrackerEventContractTest : TrackerRepositoryContractBase() {
    @Test
    fun `events are ordered by occurrence time descending`() =
        runTest {
            val tracker = givenTracker()
            val middle = givenEvent(tracker, middleEventTime)
            val newest = givenEvent(tracker, newestEventTime)
            val oldest = givenEvent(tracker, oldestEventTime)

            assertEquals(listOf(newest, middle, oldest), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun `added event belongs to the tracker and keeps its time`() =
        runTest {
            val tracker = givenTracker()

            val event = givenEvent(tracker, timeBeforeTrackerStart)

            assertEquals(tracker.id, event.trackerId)
            assertEquals(timeBeforeTrackerStart, event.occurredAt)
        }

    @Test
    fun `added event gets an id different from its tracker`() =
        runTest {
            val tracker = givenTracker()

            assertEquals("id-2", givenEvent(tracker).id)
        }

    @Test
    fun `added event takes createdAt and updatedAt from the clock`() =
        runTest {
            val tracker = givenTracker()
            clock.advanceBy(clockAdvance)

            val event = givenEvent(tracker)

            assertEquals(nowAfterAdvance, event.createdAt)
            assertEquals(nowAfterAdvance, event.updatedAt)
        }

    @Test
    fun `added event appears in observed events`() =
        runTest {
            val tracker = givenTracker()
            repo.observeEvents(tracker.id).test {
                assertTrue(awaitItem().isEmpty())
                val event = givenEvent(tracker)
                assertEquals(listOf(event), awaitItem())
            }
        }

    @Test
    fun `updating the event time re-sorts the events`() =
        runTest {
            val tracker = givenTracker()
            val older = givenEvent(tracker, oldestEventTime)
            val newer = givenEvent(tracker, newestEventTime)

            repo.updateEventTime(older.id, correctedEventTime)
            val times = repo.observeEvents(tracker.id).first().map { it.id to it.occurredAt }

            assertEquals(listOf(newer.id to newestEventTime, older.id to correctedEventTime), times)
        }

    @Test
    fun `updating the event time bumps updatedAt and keeps createdAt`() =
        runTest {
            val tracker = givenTracker()
            val event = givenEvent(tracker)
            clock.advanceBy(clockAdvance)

            repo.updateEventTime(event.id, correctedEventTime)

            val updated = repo.observeEvents(tracker.id).first().single()
            assertEquals(nowAfterAdvance, updated.updatedAt)
            assertEquals(event.createdAt, updated.createdAt)
        }

    @Test
    fun `all events flow emits again after an event is added and then moved`() =
        runTest {
            val tracker = givenTracker()
            repo.observeAllEvents().test {
                assertTrue(awaitItem().isEmpty())
                val event = givenEvent(tracker)
                assertEquals(listOf(event), awaitItem())
                repo.updateEventTime(event.id, correctedEventTime)
                assertEquals(correctedEventTime, awaitItem().single().occurredAt)
            }
        }

    @Test
    fun `events flow does not emit for another trackers events`() =
        runTest {
            val observed = givenTracker()
            val other = givenTracker()
            repo.observeEvents(observed.id).test {
                assertTrue(awaitItem().isEmpty())

                givenEvent(other)

                expectNoEvents()
            }
        }
}
