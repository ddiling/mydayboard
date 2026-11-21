package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/** The ask-for-reason switch of the editor: saved with the form, so Cancel really cancels. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerEditorReasonsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10, 30)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTrackerStore()
    private val trackers = FakeTrackerRepository(clock, ids, store)
    private val reasons = FakeTrackerReasonRepository(clock, ids, store)

    private fun viewModelFor(trackerId: String?) =
        TrackerEditorViewModel(SavedStateHandle(mapOf("trackerId" to trackerId)), trackers, reasons, clock)

    private suspend fun givenTracker(askForReason: Boolean = false): Tracker =
        trackers.createTracker("Water", TrackerType.EVENT_COUNTER, now, askForReason)

    private suspend fun storedTracker(): Tracker = trackers.observeTrackers().first().single()

    @Test
    fun `a new editor has the switch off`() {
        assertFalse(viewModelFor(null).uiState.value.askForReason)
    }

    @Test
    fun `saving a new tracker with the switch on stores the flag`() =
        runTest {
            val viewModel = viewModelFor(null)
            viewModel.onNameChange("Water")
            viewModel.onAskForReasonChange(true)

            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(storedTracker().askForReason)
        }

    @Test
    fun `editing shows the stored value of the switch`() =
        runTest {
            val viewModel = viewModelFor(givenTracker(askForReason = true).id)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.askForReason)
        }

    @Test
    fun `saving an edit after toggling the switch updates the flag`() =
        runTest {
            val viewModel = viewModelFor(givenTracker(askForReason = false).id)
            advanceUntilIdle()
            viewModel.onAskForReasonChange(true)

            viewModel.onSave()
            advanceUntilIdle()

            assertTrue(storedTracker().askForReason)
        }

    @Test
    fun `toggling the switch without saving leaves the stored flag alone`() =
        runTest {
            val viewModel = viewModelFor(givenTracker(askForReason = false).id)
            advanceUntilIdle()

            viewModel.onAskForReasonChange(true)
            advanceUntilIdle()

            assertFalse(storedTracker().askForReason)
        }

    @Test
    fun `toggling the switch does not change whether the name can be saved`() =
        runTest {
            val viewModel = viewModelFor(null)
            viewModel.onNameChange("Water")

            viewModel.onAskForReasonChange(true)

            assertTrue(viewModel.uiState.value.canSave)
        }

    @Test
    fun `editing shows how many reasons the tracker has`() =
        runTest {
            val id = givenTracker().id
            reasons.addReason(id, "I sleep better")
            reasons.addReason(id, "I save money")
            val viewModel = viewModelFor(id)

            advanceUntilIdle()

            assertEquals(2, viewModel.uiState.value.reasonCount)
        }
}
