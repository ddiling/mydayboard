package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for adding reasons and observing them, including unknown and deleted trackers. */
abstract class TrackerReasonContractTest : TrackerReasonContractBase() {
    @Test
    fun `added reason has the injected id, text and clock times`() =
        runTest {
            val tracker = givenTracker()

            val reason = givenReason(tracker, "Coffee after lunch")

            assertEquals(TrackerReason("id-2", tracker.id, "Coffee after lunch", now, now), reason)
        }

    @Test
    fun `adding a reason to an unknown tracker returns null`() =
        runTest {
            assertNull(reasons.addReason("unknown", "Water"))
        }

    @Test
    fun `adding a reason to a deleted tracker returns null`() =
        runTest {
            assertNull(reasons.addReason(givenDeletedTracker().id, "Water"))
        }

    @Test
    fun `reasons are listed oldest first`() =
        runTest {
            val tracker = givenTracker()
            val first = givenReason(tracker)
            clock.advanceBy(clockAdvance)
            val second = givenReason(tracker)

            assertEquals(listOf(first, second), reasons.observeReasons(tracker.id).first())
        }

    @Test
    fun `reasons of a deleted tracker are hidden`() =
        runTest {
            val tracker = givenTracker()
            givenReason(tracker)
            repo.deleteTracker(tracker.id)

            assertTrue(reasons.observeReasons(tracker.id).first().isEmpty())
        }

    @Test
    fun `observed reasons do not emit again after an unrelated event write`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)

            reasons.observeReasons(tracker.id).test {
                assertEquals(listOf(reason), awaitItem())
                givenEvent(tracker)
                expectNoEvents()
            }
        }
}
