package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for creating, ordering and renaming trackers. */
abstract class TrackerContractTest : TrackerRepositoryContractBase() {
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
                assertEquals(emptyList<Any>(), awaitItem())
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
