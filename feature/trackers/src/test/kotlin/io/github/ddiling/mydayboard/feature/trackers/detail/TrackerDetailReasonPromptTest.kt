package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.lifecycle.SavedStateHandle
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerReasonRepository
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.FakeTrackerStore
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPrompt
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPromptKind
import io.github.ddiling.mydayboard.feature.trackers.reason.ReasonPrompter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import kotlin.random.Random

/** The detail screen's +1 and Reset use the same dialog and logic as the dashboard tiles. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerDetailReasonPromptTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTrackerStore()
    private val trackers = FakeTrackerRepository(clock, ids, store)
    private val reasons = FakeTrackerReasonRepository(clock, ids, store)

    private fun viewModelFor(trackerId: String) =
        TrackerDetailViewModel(
            SavedStateHandle(mapOf("trackerId" to trackerId)),
            trackers,
            clock,
            ReasonPrompter(trackers, reasons, Random(1)),
        )

    private suspend fun eventTimes(trackerId: String): List<Instant> =
        trackers.observeEvents(trackerId).first().map { it.occurredAt }

    @Test
    fun `plus one with the switch on opens the dialog and logs nothing yet`() =
        runTest {
            val water = trackers.createTracker("Water", TrackerType.EVENT_COUNTER, now, askForReason = true)
            reasons.addReason(water.id, "I save money")
            val viewModel = viewModelFor(water.id)

            viewModel.onPlusOne()
            runCurrent()

            val prompt = viewModel.prompt.value as ReasonPrompt.WithReason
            assertEquals(ReasonPromptKind.COUNTER, prompt.kind)
            assertEquals(emptyList<Instant>(), eventTimes(water.id))
        }

    @Test
    fun `reset with the switch on uses the days since wording and logs on log anyway`() =
        runTest {
            val sugar = trackers.createTracker("No sweets", TrackerType.DAYS_SINCE, now, askForReason = true)
            reasons.addReason(sugar.id, "I sleep better")
            val viewModel = viewModelFor(sugar.id)
            viewModel.onReset()
            runCurrent()
            val kind = viewModel.prompt.value?.kind

            viewModel.onPromptLogAnyway()
            runCurrent()

            assertEquals(ReasonPromptKind.DAYS_SINCE, kind)
            assertEquals(listOf(now), eventTimes(sugar.id))
        }

    @Test
    fun `plus one with the switch off logs at once`() =
        runTest {
            val coffee = trackers.createTracker("Coffee", TrackerType.EVENT_COUNTER, now)
            val viewModel = viewModelFor(coffee.id)

            viewModel.onPlusOne()
            runCurrent()

            assertEquals(listOf(now), eventTimes(coffee.id))
            assertNull(viewModel.prompt.value)
        }

    @Test
    fun `wait on the detail screen closes the dialog and logs nothing`() =
        runTest {
            val water = trackers.createTracker("Water", TrackerType.EVENT_COUNTER, now, askForReason = true)
            val viewModel = viewModelFor(water.id)
            viewModel.onPlusOne()
            runCurrent()

            viewModel.prompter.dismiss()

            assertNull(viewModel.prompt.value)
            assertEquals(emptyList<Instant>(), eventTimes(water.id))
        }
}
