package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for creating, ordering and renaming trackers. */
abstract class TrackerContractTest : TrackerRepositoryContractBase() {
    /** More than 9, so that ids compared as text (`id-10` before `id-2`) differ from creation order. */
    private val tiedCount = 10

    @Test
    fun `created tracker gets a generated id`() =
        runTest {
            assertEquals("id-1", givenTracker().id)
        }

    @Test
    fun `created tracker keeps the given name type and start`() =
        runTest {
            val tracker = repo.createTracker("Water", TrackerType.DAYS_SINCE, timeBeforeTrackerStart)

            assertEquals("Water", tracker.name)
            assertEquals(TrackerType.DAYS_SINCE, tracker.type)
            assertEquals(timeBeforeTrackerStart, tracker.startedAt)
        }

    @Test
    fun `a new tracker does not ask for a reason`() =
        runTest {
            assertFalse(givenTracker().askForReason)
        }

    @Test
    fun `created tracker keeps the ask-for-reason flag it was given`() =
        runTest {
            val tracker = repo.createTracker("Water", TrackerType.EVENT_COUNTER, now, askForReason = true)

            assertEquals(tracker, repo.observeTracker(tracker.id).first())
            assertTrue(tracker.askForReason)
        }

    @Test
    fun `created tracker takes createdAt and updatedAt from the clock`() =
        runTest {
            val tracker = givenTracker()

            assertEquals(now, tracker.createdAt)
            assertEquals(now, tracker.updatedAt)
        }

    @Test
    fun `created tracker appears in observed trackers`() =
        runTest {
            repo.observeTrackers().test {
                assertEquals(emptyList<Tracker>(), awaitItem())
                val tracker = givenTracker()
                assertEquals(listOf(tracker), awaitItem())
            }
        }

    @Test
    fun `trackers are ordered by creation time ascending`() =
        runTest {
            val first = givenTracker()
            clock.advanceBy(clockAdvance)
            val second = givenTracker()

            assertEquals(listOf(first, second), repo.observeTrackers().first())
        }

    @Test
    fun `trackers created at the same time are ordered by id ascending`() =
        runTest {
            repeat(tiedCount) { givenTracker() }

            val ids = repo.observeTrackers().first().map { it.id }

            assertEquals(ids.sorted(), ids)
        }

    @Test
    fun `renaming changes the name`() =
        runTest {
            val tracker = givenTracker()

            repo.renameTracker(tracker.id, "Coffee")

            assertEquals("Coffee", repo.observeTracker(tracker.id).first()?.name)
        }

    @Test
    fun `renaming bumps updatedAt`() =
        runTest {
            val tracker = givenTracker()
            clock.advanceBy(clockAdvance)

            repo.renameTracker(tracker.id, "Coffee")

            assertEquals(nowAfterAdvance, repo.observeTracker(tracker.id).first()?.updatedAt)
        }

    @Test
    fun `renaming keeps createdAt`() =
        runTest {
            val tracker = givenTracker()
            clock.advanceBy(clockAdvance)

            repo.renameTracker(tracker.id, "Coffee")

            assertEquals(now, repo.observeTracker(tracker.id).first()?.createdAt)
        }
}
