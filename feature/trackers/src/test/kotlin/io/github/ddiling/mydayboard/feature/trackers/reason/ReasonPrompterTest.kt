package io.github.ddiling.mydayboard.feature.trackers.reason

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerReasonRepository
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.FakeTrackerStore
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class ReasonPrompterTest {
    private val clock = MutableClock(warsaw(2026, 3, 2, 10), TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTrackerStore()
    private val trackers = FakeTrackerRepository(clock, ids, store)
    private val reasons = FakeTrackerReasonRepository(clock, ids, store)
    private val prompter = ReasonPrompter(trackers, reasons, Random(3))

    private suspend fun givenAskingTracker(type: TrackerType = TrackerType.EVENT_COUNTER) =
        trackers.createTracker("Water", type, clock.instant(), askForReason = true)

    @Test
    fun `a tracker with the switch off does not ask`() =
        runTest {
            val coffee = trackers.createTracker("Coffee", TrackerType.EVENT_COUNTER, clock.instant())

            val asked = prompter.request(coffee.id, ReasonPromptKind.COUNTER)

            assertFalse(asked)
            assertNull(prompter.prompt.value)
        }

    @Test
    fun `a missing tracker does not ask`() =
        runTest {
            assertFalse(prompter.request("missing", ReasonPromptKind.COUNTER))
        }

    @Test
    fun `the switch on with reasons opens a prompt with one of them`() =
        runTest {
            val water = givenAskingTracker()
            val saved = reasons.addReason(water.id, "I sleep better")

            val asked = prompter.request(water.id, ReasonPromptKind.COUNTER)

            val prompt = prompter.prompt.value as ReasonPrompt.WithReason
            assertTrue(asked)
            assertEquals(saved, prompt.reason)
            assertEquals("Water", prompt.trackerName)
        }

    @Test
    fun `the switch on without reasons opens the no reasons prompt`() =
        runTest {
            val water = givenAskingTracker()

            prompter.request(water.id, ReasonPromptKind.COUNTER)

            assertEquals(ReasonPrompt.NoReasons(water.id, "Water", ReasonPromptKind.COUNTER), prompter.prompt.value)
        }

    @Test
    fun `the flag is read at tap time, so turning it on takes effect at the next tap`() =
        runTest {
            val water = trackers.createTracker("Water", TrackerType.EVENT_COUNTER, clock.instant())
            assertFalse(prompter.request(water.id, ReasonPromptKind.COUNTER))

            reasons.setAskForReason(water.id, enabled = true)

            assertTrue(prompter.request(water.id, ReasonPromptKind.COUNTER))
        }

    @Test
    fun `another reason shows a different one`() =
        runTest {
            val water = givenAskingTracker()
            reasons.addReason(water.id, "I sleep better")
            reasons.addReason(water.id, "I save money")
            prompter.request(water.id, ReasonPromptKind.COUNTER)
            val first = (prompter.prompt.value as ReasonPrompt.WithReason).reason

            prompter.another()

            assertNotEquals(first, (prompter.prompt.value as ReasonPrompt.WithReason).reason)
        }

    @Test
    fun `with one reason another reason is not offered`() =
        runTest {
            val water = givenAskingTracker()
            reasons.addReason(water.id, "I sleep better")

            prompter.request(water.id, ReasonPromptKind.COUNTER)

            assertFalse((prompter.prompt.value as ReasonPrompt.WithReason).canSwap)
        }

    @Test
    fun `with two reasons another reason is offered`() =
        runTest {
            val water = givenAskingTracker()
            reasons.addReason(water.id, "I sleep better")
            reasons.addReason(water.id, "I save money")

            prompter.request(water.id, ReasonPromptKind.COUNTER)

            assertTrue((prompter.prompt.value as ReasonPrompt.WithReason).canSwap)
        }

    @Test
    fun `dismiss closes the prompt and changes no reason`() =
        runTest {
            val water = givenAskingTracker()
            reasons.addReason(water.id, "I sleep better")
            prompter.request(water.id, ReasonPromptKind.COUNTER)

            prompter.dismiss()

            assertNull(prompter.prompt.value)
            assertEquals(1, reasons.observeReasons(water.id).first().size)
        }

    @Test
    fun `confirm returns the open prompt once and closes it`() =
        runTest {
            val water = givenAskingTracker(TrackerType.DAYS_SINCE)
            prompter.request(water.id, ReasonPromptKind.DAYS_SINCE)

            val confirmed = prompter.confirm()

            assertEquals(ReasonPromptKind.DAYS_SINCE, confirmed?.kind)
            assertNull(prompter.confirm())
            assertNull(prompter.prompt.value)
        }
}
