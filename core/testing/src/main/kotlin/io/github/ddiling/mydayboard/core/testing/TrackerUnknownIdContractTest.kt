package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for ids that never existed: operations on them change nothing and do not throw. */
abstract class TrackerUnknownIdContractTest : TrackerRepositoryContractBase() {
    @Test
    fun `observing an unknown tracker emits null`() =
        runTest {
            assertNull(repo.observeTracker("nope").first())
        }

    @Test
    fun `observing events of an unknown tracker emits an empty list`() =
        runTest {
            assertTrue(repo.observeEvents("nope").first().isEmpty())
        }

    @Test
    fun `renaming an unknown tracker changes nothing`() =
        runTest {
            val tracker = givenTracker()

            repo.renameTracker("nope", "Coffee")

            assertEquals(listOf(tracker), repo.observeTrackers().first())
        }

    @Test
    fun `deleting an unknown tracker changes nothing`() =
        runTest {
            val tracker = givenTracker()

            repo.deleteTracker("nope")

            assertEquals(listOf(tracker), repo.observeTrackers().first())
        }

    @Test
    fun `adding an event to an unknown tracker returns null`() =
        runTest {
            assertNull(repo.addEvent("nope", now))
        }

    @Test
    fun `adding an event to an unknown tracker stores nothing`() =
        runTest {
            repo.addEvent("nope", now)

            assertTrue(repo.observeAllEvents().first().isEmpty())
        }

    @Test
    fun `updating an unknown event changes nothing`() =
        runTest {
            val event = givenEvent(givenTracker())

            repo.updateEventTime("nope", newestEventTime)

            assertEquals(listOf(event), repo.observeAllEvents().first())
        }

    @Test
    fun `deleting an unknown event changes nothing`() =
        runTest {
            val event = givenEvent(givenTracker())

            repo.deleteEvent("nope")

            assertEquals(listOf(event), repo.observeAllEvents().first())
        }

    @Test
    fun `restoring an unknown event changes nothing`() =
        runTest {
            val event = givenEvent(givenTracker())

            repo.restoreEvent("nope")

            assertEquals(listOf(event), repo.observeAllEvents().first())
        }
}
