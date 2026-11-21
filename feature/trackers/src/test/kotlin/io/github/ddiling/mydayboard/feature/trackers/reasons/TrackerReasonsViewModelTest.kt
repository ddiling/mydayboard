package io.github.ddiling.mydayboard.feature.trackers.reasons

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.MAX_REASON_LENGTH
import io.github.ddiling.mydayboard.core.domain.tracker.ReasonTextValidation
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
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerReasonsViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val ids = SequentialIdGenerator()
    private val store = FakeTrackerStore()
    private val trackers = FakeTrackerRepository(clock, ids, store)
    private val reasons = FakeTrackerReasonRepository(clock, ids, store)

    private suspend fun givenTracker(): String = trackers.createTracker("No sweets", TrackerType.DAYS_SINCE, now).id

    private suspend fun givenReasons(trackerId: String): List<String> =
        listOf("I sleep better", "I save money", "I feel calmer").map {
            clock.advanceBy(Duration.ofMinutes(1))
            checkNotNull(reasons.addReason(trackerId, it)).id
        }

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private fun viewModelFor(trackerId: String) =
        TrackerReasonsViewModel(SavedStateHandle(mapOf("trackerId" to trackerId)), trackers, reasons)

    private fun TrackerReasonsViewModel.content(): TrackerReasonsUiState.Content =
        uiState.value as TrackerReasonsUiState.Content

    @Test
    fun `an unknown tracker shows not found`() =
        runTest {
            val viewModel = viewModelFor("missing")

            viewModel.uiState.test {
                runCurrent()
                assertEquals(TrackerReasonsUiState.Loading, awaitItem())
                assertEquals(TrackerReasonsUiState.NotFound, awaitItem())
            }
        }

    @Test
    fun `a tracker without reasons shows an empty list`() =
        runTest {
            val viewModel = viewModelFor(givenTracker())

            viewModel.uiState.test {
                runCurrent()
                skipItems(1)
                assertEquals(TrackerReasonsUiState.Content("No sweets", emptyList()), awaitItem())
            }
        }

    @Test
    fun `reasons are listed oldest first`() =
        runTest {
            val trackerId = givenTracker()
            givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)

            viewModel.uiState.test {
                runCurrent()
                skipItems(1)
                val texts = (awaitItem() as TrackerReasonsUiState.Content).reasons.map { it.text }
                assertEquals(listOf("I sleep better", "I save money", "I feel calmer"), texts)
            }
        }

    @Test
    fun `saving valid text adds the trimmed reason`() =
        runTest {
            val trackerId = givenTracker()
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onAddClick()
                viewModel.onTextChange("  I sleep better ")

                viewModel.onSave()
                runCurrent()

                val texts = reasons.observeReasons(trackerId).first().map { it.text }
                assertEquals(listOf("I sleep better"), texts)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saving valid text closes the dialog`() =
        runTest {
            val trackerId = givenTracker()
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onAddClick()
                viewModel.onTextChange("I sleep better")

                viewModel.onSave()
                runCurrent()

                assertNull(viewModel.content().editor)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `blank text cannot be saved`() =
        runTest {
            val trackerId = givenTracker()
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onAddClick()
                viewModel.onTextChange("   ")
                runCurrent()

                assertEquals(false, viewModel.content().editor?.canSave)
                viewModel.onSave()
                runCurrent()

                assertTrue(reasons.observeReasons(trackerId).first().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `text over the limit shows the too-long error and stores nothing`() =
        runTest {
            val trackerId = givenTracker()
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onAddClick()
                viewModel.onTextChange("x".repeat(MAX_REASON_LENGTH + 1))
                viewModel.onSave()
                runCurrent()

                val editor = viewModel.content().editor
                assertEquals(ReasonTextValidation.TooLong(MAX_REASON_LENGTH), editor?.validation)
                assertTrue(reasons.observeReasons(trackerId).first().isEmpty())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `tapping a reason opens the dialog with its current text`() =
        runTest {
            val trackerId = givenTracker()
            val (first) = givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()

                viewModel.onEditClick(first)
                runCurrent()

                assertEquals(ReasonEditor(first, "I sleep better"), viewModel.content().editor)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `saving an edit changes the text of that reason`() =
        runTest {
            val trackerId = givenTracker()
            val (first) = givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onEditClick(first)
                runCurrent()
                viewModel.onTextChange("I wake up rested")

                viewModel.onSave()
                runCurrent()

                val texts = reasons.observeReasons(trackerId).first().map { it.text }
                assertEquals(listOf("I wake up rested", "I save money", "I feel calmer"), texts)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `swipe delete hides the reason and offers undo`() =
        runTest {
            val trackerId = givenTracker()
            val (first, second, third) = givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()

                viewModel.onSwipeDelete(first)
                runCurrent()

                val content = viewModel.content()
                assertEquals(listOf(second, third), content.reasons.map { it.id })
                assertEquals(first, content.pendingUndoId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `undo brings the reason back in its old place`() =
        runTest {
            val trackerId = givenTracker()
            val (first) = givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onSwipeDelete(first)
                runCurrent()
                clock.advanceBy(Duration.ofHours(1))

                viewModel.onUndoDelete()
                runCurrent()

                val content = viewModel.content()
                assertEquals(first, content.reasons.first().id)
                assertNull(content.pendingUndoId)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `a newer delete replaces the older undo`() =
        runTest {
            val trackerId = givenTracker()
            val (first, second) = givenReasons(trackerId)
            val viewModel = viewModelFor(trackerId)
            viewModel.uiState.test {
                runCurrent()
                viewModel.onSwipeDelete(first)
                runCurrent()
                viewModel.onSwipeDelete(second)
                runCurrent()

                viewModel.onUndoDelete()
                runCurrent()

                val ids = viewModel.content().reasons.map { it.id }
                assertTrue(second in ids)
                assertTrue(first !in ids)
                cancelAndIgnoreRemainingEvents()
            }
        }
}
