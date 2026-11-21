package io.github.ddiling.mydayboard.core.testing

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for the ask-for-reason flag and for adding and observing reasons. */
abstract class TrackerReasonContractTest : TrackerReasonContractBase() {
    /** More than 9, so that ids compared as text (`id-10` before `id-2`) differ from creation order. */
    private val tiedCount = 10

    @Test
    fun `turning the flag on shows in the observed tracker and bumps updatedAt`() =
        runTest {
            val tracker = givenTracker()
            clock.advanceBy(clockAdvance)

            reasons.setAskForReason(tracker.id, true)

            assertEquals(
                tracker.copy(askForReason = true, updatedAt = nowAfterAdvance),
                repo.observeTracker(tracker.id).first(),
            )
        }

    @Test
    fun `setting the flag on a deleted tracker does not bring it back`() =
        runTest {
            val tracker = givenDeletedTracker()

            reasons.setAskForReason(tracker.id, true)

            assertNull(repo.observeTracker(tracker.id).first())
        }

    @Test
    fun `setting the flag on an unknown tracker changes nothing`() =
        runTest {
            reasons.setAskForReason("unknown", true)

            assertTrue(repo.observeTrackers().first().isEmpty())
        }

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
    fun `reasons with the same createdAt are listed by id`() =
        runTest {
            val tracker = givenTracker()
            repeat(tiedCount) { givenReason(tracker) }

            val listedIds = reasons.observeReasons(tracker.id).first().map { it.id }

            assertEquals(listedIds.sorted(), listedIds)
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
    fun `observed reasons do not emit again after a write that leaves the list equal`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)

            reasons.observeReasons(tracker.id).test {
                assertEquals(listOf(reason), awaitItem())
                reasons.setAskForReason(tracker.id, true)
                // Room re-runs the query on its own thread, so the next real change proves nothing came in between.
                val next = givenReason(tracker, "Coffee after lunch")
                assertEquals(listOf(reason, next), awaitItem())
                expectNoEvents()
            }
        }
}
