package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Behaviour every [TrackerRepository] must have. The in-memory fake and the Room implementation both extend
 * this class, which is what proves they behave identically.
 */
abstract class TrackerRepositoryContractTest {
    abstract fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository

    private val start = Instant.parse("2025-03-01T10:00:00Z")
    private val clock = MutableClock(start, ZoneOffset.UTC)
    private val repo by lazy { createRepository(clock, SequentialIdGenerator()) }

    private fun later(minutes: Long) = start.plus(Duration.ofMinutes(minutes))

    @Test
    fun `created tracker has a generated id and timestamps from the clock`() =
        runTest {
            val tracker = repo.createTracker("Water", TrackerType.EVENT_COUNTER, later(-60))

            assertEquals("id-1", tracker.id)
            assertEquals("Water", tracker.name)
            assertEquals(later(-60), tracker.startedAt)
            assertEquals(start, tracker.createdAt)
            assertEquals(start, tracker.updatedAt)
        }

    @Test
    fun `created tracker appears in observed trackers`() =
        runTest {
            repo.observeTrackers().test {
                assertEquals(emptyList<Any>(), awaitItem())
                val tracker = repo.createTracker("Water", TrackerType.EVENT_COUNTER, start)
                assertEquals(listOf(tracker), awaitItem())
            }
        }

    @Test
    fun `trackers are ordered by creation time ascending`() =
        runTest {
            val first = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(Duration.ofMinutes(1))
            val second = repo.createTracker("B", TrackerType.DAYS_SINCE, start)

            assertEquals(listOf(first, second), repo.observeTrackers().first())
        }

    @Test
    fun `events are ordered by occurrence time descending`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val middle = repo.addEvent(tracker.id, later(5))!!
            val newest = repo.addEvent(tracker.id, later(9))!!
            val oldest = repo.addEvent(tracker.id, later(1))!!

            assertEquals(listOf(newest, middle, oldest), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun `renaming bumps updatedAt and keeps createdAt`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(Duration.ofMinutes(3))

            repo.renameTracker(tracker.id, "B")

            val renamed = repo.observeTracker(tracker.id).first()!!
            assertEquals("B", renamed.name)
            assertEquals(start, renamed.createdAt)
            assertEquals(later(3), renamed.updatedAt)
        }

    @Test
    fun `deleted tracker disappears from the trackers list`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            assertEquals(emptyList<Any>(), repo.observeTrackers().first())
        }

    @Test
    fun `observing a deleted tracker emits null`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.observeTracker(tracker.id).test {
                assertEquals(tracker, awaitItem())
                repo.deleteTracker(tracker.id)
                assertNull(awaitItem())
            }
        }

    @Test
    fun `observing a missing tracker emits null`() =
        runTest {
            assertNull(repo.observeTracker("nope").first())
        }

    @Test
    fun `deleting a tracker removes its events from all events but keeps other trackers events`() =
        runTest {
            val gone = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val kept = repo.createTracker("B", TrackerType.EVENT_COUNTER, start)
            repo.addEvent(gone.id, later(1))
            val keptEvent = repo.addEvent(kept.id, later(2))!!

            repo.deleteTracker(gone.id)

            assertEquals(listOf(keptEvent), repo.observeAllEvents().first())
        }

    @Test
    fun `events of a deleted tracker are empty`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.addEvent(tracker.id, later(1))
            repo.deleteTracker(tracker.id)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }

    @Test
    fun `events of a missing tracker are empty`() =
        runTest {
            assertTrue(repo.observeEvents("nope").first().isEmpty())
        }

    @Test
    fun `added event belongs to the tracker and takes timestamps from the clock`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            clock.advanceBy(Duration.ofMinutes(2))

            val event = repo.addEvent(tracker.id, later(-30))!!

            assertEquals(tracker.id, event.trackerId)
            assertEquals(later(-30), event.occurredAt)
            assertEquals(later(2), event.createdAt)
            assertEquals(later(2), event.updatedAt)
            assertEquals(listOf(event), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun `updating an event time re-sorts the list and bumps updatedAt`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val older = repo.addEvent(tracker.id, later(1))!!
            val newer = repo.addEvent(tracker.id, later(5))!!
            clock.advanceBy(Duration.ofMinutes(10))

            repo.updateEventTime(older.id, later(8))

            val moved = older.copy(occurredAt = later(8), updatedAt = later(10))
            assertEquals(listOf(moved, newer), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun `deleted event disappears`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, later(1))!!
            repo.observeEvents(tracker.id).test {
                assertEquals(listOf(event), awaitItem())
                repo.deleteEvent(event.id)
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `renaming or deleting an unknown tracker changes nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)

            repo.renameTracker("nope", "X")
            repo.deleteTracker("nope")

            assertEquals(listOf(tracker), repo.observeTrackers().first())
        }

    @Test
    fun `renaming or deleting an already deleted tracker changes nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            repo.renameTracker(tracker.id, "X")
            repo.deleteTracker(tracker.id)

            assertEquals(emptyList<Any>(), repo.observeTrackers().first())
            assertNull(repo.observeTracker(tracker.id).first())
        }

    @Test
    fun `adding an event to an unknown tracker returns null and stores nothing`() =
        runTest {
            assertNull(repo.addEvent("nope", start))
            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `adding an event to a deleted tracker returns null and stores nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.deleteTracker(tracker.id)

            assertNull(repo.addEvent(tracker.id, start))
            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `updating or deleting an unknown event changes nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, later(1))!!

            repo.updateEventTime("nope", later(9))
            repo.deleteEvent("nope")

            assertEquals(listOf(event), repo.observeEvents(tracker.id).first())
        }

    @Test
    fun `updating or deleting an event of a deleted tracker changes nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, later(1))!!
            repo.deleteTracker(tracker.id)

            repo.updateEventTime(event.id, later(9))
            repo.deleteEvent(event.id)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `updating or deleting an already deleted event changes nothing`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val event = repo.addEvent(tracker.id, later(1))!!
            repo.deleteEvent(event.id)

            repo.updateEventTime(event.id, later(9))
            repo.deleteEvent(event.id)

            assertTrue(repo.observeEvents(tracker.id).first().isEmpty())
        }

    @Test
    fun `all events flow emits again after each change`() =
        runTest {
            val tracker = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            repo.observeAllEvents().test {
                assertTrue(awaitItem().isEmpty())
                val event = repo.addEvent(tracker.id, later(1))!!
                assertEquals(listOf(event), awaitItem())
                repo.updateEventTime(event.id, later(2))
                assertEquals(later(2), awaitItem().single().occurredAt)
                repo.deleteEvent(event.id)
                assertTrue(awaitItem().isEmpty())
            }
        }

    @Test
    fun `events flow does not emit for changes to another trackers events`() =
        runTest {
            val observed = repo.createTracker("A", TrackerType.EVENT_COUNTER, start)
            val other = repo.createTracker("B", TrackerType.EVENT_COUNTER, start)
            repo.observeEvents(observed.id).test {
                assertTrue(awaitItem().isEmpty())

                repo.addEvent(other.id, later(1))

                expectNoEvents()
            }
        }
}
