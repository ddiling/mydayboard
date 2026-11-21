package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for editing, deleting and restoring reasons, including unknown and deleted ids. */
abstract class TrackerReasonChangeContractTest : TrackerReasonContractBase() {
    @Test
    fun `updating a reason changes its text and updatedAt`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)
            clock.advanceBy(clockAdvance)

            reasons.updateReason(reason.id, "Coffee")

            assertEquals(
                listOf(reason.copy(text = "Coffee", updatedAt = nowAfterAdvance)),
                reasons.observeReasons(tracker.id).first(),
            )
        }

    @Test
    fun `updating an unknown reason changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)

            reasons.updateReason("unknown", "Coffee")

            assertEquals(listOf(reason), reasons.observeReasons(tracker.id).first())
        }

    @Test
    fun `a deleted reason is hidden`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)

            reasons.deleteReason(reason.id)

            assertTrue(reasons.observeReasons(tracker.id).first().isEmpty())
        }

    @Test
    fun `restoring a reason brings it back and bumps updatedAt from the clock`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenDeletedReason(tracker)
            clock.advanceBy(clockAdvance)

            reasons.restoreReason(reason.id)

            assertEquals(
                listOf(reason.copy(updatedAt = nowAfterAdvance)),
                reasons.observeReasons(tracker.id).first(),
            )
        }

    @Test
    fun `restoring a reason that is not deleted changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenReason(tracker)
            clock.advanceBy(clockAdvance)

            reasons.restoreReason(reason.id)

            assertEquals(listOf(reason), reasons.observeReasons(tracker.id).first())
        }

    @Test
    fun `restoring a reason of a deleted tracker changes nothing`() =
        runTest {
            val tracker = givenTracker()
            val reason = givenDeletedReason(tracker)
            repo.deleteTracker(tracker.id)

            reasons.restoreReason(reason.id)

            assertTrue(reasons.observeReasons(tracker.id).first().isEmpty())
        }
}
