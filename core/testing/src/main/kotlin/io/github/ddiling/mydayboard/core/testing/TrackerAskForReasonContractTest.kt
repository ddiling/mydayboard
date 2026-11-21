package io.github.ddiling.mydayboard.core.testing

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Contract for the ask-for-reason flag, including unknown and deleted trackers. */
abstract class TrackerAskForReasonContractTest : TrackerReasonContractBase() {
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
}
