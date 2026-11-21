package io.github.ddiling.mydayboard.feature.trackers.dashboard

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.ObserveTrackerSummariesUseCase
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
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
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

/** A tap on a tracker that asks for a reason opens the dialog first; only "Log anyway" logs. */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackersDashboardReasonPromptTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val tapTime = warsaw(2026, 3, 2, 10)
    private val laterConfirmTime = warsaw(2026, 3, 2, 10, 5)
    private val clock = MutableClock(tapTime, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTrackerStore()
    private val trackers = FakeTrackerRepository(clock, ids, store)
    private val reasons = FakeTrackerReasonRepository(clock, ids, store)

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private val viewModel by lazy {
        TrackersDashboardCardViewModel(
            ObserveTrackerSummariesUseCase(trackers, clock),
            trackers,
            clock,
            ReasonPrompter(trackers, reasons, Random(1)),
        )
    }

    private suspend fun givenAskingTracker(
        type: TrackerType = TrackerType.EVENT_COUNTER,
        reason: String? = "I sleep better",
    ): Tracker {
        val tracker = trackers.createTracker("Water", type, tapTime, askForReason = true)
        reason?.let { reasons.addReason(tracker.id, it) }
        return tracker
    }

    private suspend fun eventTimes(trackerId: String): List<Instant> =
        trackers.observeEvents(trackerId).first().map { it.occurredAt }

    private fun TestScope.tapPlusOne(trackerId: String) {
        viewModel.onPlusOne(trackerId)
        runCurrent()
    }

    @Test
    fun `a tracker with the switch off logs at once and shows no dialog`() =
        runTest {
            val coffee = trackers.createTracker("Coffee", TrackerType.EVENT_COUNTER, tapTime)

            tapPlusOne(coffee.id)

            assertEquals(listOf(tapTime), eventTimes(coffee.id))
            assertNull(viewModel.prompt.value)
        }

    @Test
    fun `a tracker with the switch on opens the dialog and logs nothing yet`() =
        runTest {
            val water = givenAskingTracker()

            tapPlusOne(water.id)

            assertEquals("I sleep better", (viewModel.prompt.value as ReasonPrompt.WithReason).reason.text)
            assertEquals(emptyList<Instant>(), eventTimes(water.id))
        }

    @Test
    fun `log anyway logs an event stamped when it is chosen, not when the tracker was tapped`() =
        runTest {
            val water = givenAskingTracker()
            tapPlusOne(water.id)
            clock.now = laterConfirmTime

            viewModel.onPromptLogAnyway()
            runCurrent()

            assertEquals(listOf(laterConfirmTime), eventTimes(water.id))
            assertNull(viewModel.prompt.value)
        }

    @Test
    fun `undo after log anyway removes the logged event`() =
        runTest {
            val water = givenAskingTracker()
            tapPlusOne(water.id)
            viewModel.onPromptLogAnyway()
            runCurrent()

            viewModel.onUndo()
            runCurrent()

            assertEquals(emptyList<Instant>(), eventTimes(water.id))
        }

    @Test
    fun `wait, a tap outside and Back close the dialog and save no event`() =
        runTest {
            val water = givenAskingTracker()
            tapPlusOne(water.id)

            viewModel.onPromptWait()
            runCurrent()

            assertNull(viewModel.prompt.value)
            assertEquals(emptyList<Instant>(), eventTimes(water.id))
            assertEquals(1, reasons.observeReasons(water.id).first().size)
        }

    @Test
    fun `the switch on with no reasons opens the no reasons dialog`() =
        runTest {
            val water = givenAskingTracker(reason = null)

            tapPlusOne(water.id)

            assertEquals(true, viewModel.prompt.value is ReasonPrompt.NoReasons)
        }

    @Test
    fun `log anyway from the no reasons dialog logs the event`() =
        runTest {
            val water = givenAskingTracker(reason = null)
            tapPlusOne(water.id)

            viewModel.onPromptLogAnyway()
            runCurrent()

            assertEquals(listOf(tapTime), eventTimes(water.id))
        }

    @Test
    fun `reset of a days since tracker asks first`() =
        runTest {
            val sugar = givenAskingTracker(TrackerType.DAYS_SINCE)

            viewModel.onReset(sugar.id)
            runCurrent()

            assertEquals(ReasonPromptKind.DAYS_SINCE, viewModel.prompt.value?.kind)
            assertEquals(emptyList<Instant>(), eventTimes(sugar.id))
        }

    @Test
    fun `log anyway after a reset adds the reset event`() =
        runTest {
            val sugar = givenAskingTracker(TrackerType.DAYS_SINCE)
            viewModel.onReset(sugar.id)
            runCurrent()

            viewModel.onPromptLogAnyway()
            runCurrent()

            assertEquals(listOf(tapTime), eventTimes(sugar.id))
        }

    @Test
    fun `adding the first reason closes the dialog and dismisses the pending undo`() =
        runTest {
            val coffee = trackers.createTracker("Coffee", TrackerType.EVENT_COUNTER, tapTime)
            val water = givenAskingTracker(reason = null)
            viewModel.uiState.test {
                runCurrent()
                skipItems(2)
                tapPlusOne(coffee.id)
                assertNotNull((expectMostRecentItem() as TrackersDashboardCardUiState.Content).pendingUndo)
                tapPlusOne(water.id)

                viewModel.onPromptAddFirstReason()
                runCurrent()

                assertNull(viewModel.prompt.value)
                assertNull((expectMostRecentItem() as TrackersDashboardCardUiState.Content).pendingUndo)
            }
        }

    @Test
    fun `log anyway for a tracker deleted while the dialog was open logs nothing`() =
        runTest {
            val water = givenAskingTracker()
            tapPlusOne(water.id)
            trackers.deleteTracker(water.id)

            viewModel.onPromptLogAnyway()
            runCurrent()

            assertEquals(emptyList<Instant>(), eventTimes(water.id))
        }
}
