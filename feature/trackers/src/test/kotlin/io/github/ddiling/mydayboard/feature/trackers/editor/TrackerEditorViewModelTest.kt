package io.github.ddiling.mydayboard.feature.trackers.editor

import androidx.lifecycle.SavedStateHandle
import io.github.ddiling.mydayboard.core.domain.tracker.MAX_TRACKER_NAME_LENGTH
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerNameValidation
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.LocalDate

/** Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerEditorViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10, 30)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val today = LocalDate.of(2026, 3, 2)

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private val createViewModel by lazy { viewModelFor(null) }

    private fun viewModelFor(trackerId: String?) =
        TrackerEditorViewModel(
            SavedStateHandle(mapOf("trackerId" to trackerId)),
            repository,
            clock,
        )

    private suspend fun trackers(): List<Tracker> = repository.observeTrackers().first()

    private suspend fun givenTracker(name: String = "Water"): Tracker =
        repository.createTracker(name, TrackerType.EVENT_COUNTER, now)

    @Test
    fun `a new editor is an empty counter form starting today with no error and nothing to save`() {
        val state = createViewModel.uiState.value

        assertEquals("", state.name)
        assertEquals(TrackerType.EVENT_COUNTER, state.type)
        assertEquals(today, state.startDate)
        assertNull(state.nameError)
        assertFalse(state.canSave)
        assertFalse(state.isEditing)
    }

    @Test
    fun `an untouched empty field shows no error but a blank edit does`() {
        createViewModel.onNameChange("  ")

        assertEquals(TrackerNameValidation.Blank, createViewModel.uiState.value.nameError)
    }

    @Test
    fun `a name over the limit shows the limit as an error and cannot be saved`() {
        createViewModel.onNameChange("x".repeat(MAX_TRACKER_NAME_LENGTH + 1))

        val state = createViewModel.uiState.value
        assertEquals(TrackerNameValidation.TooLong(MAX_TRACKER_NAME_LENGTH), state.nameError)
        assertFalse(state.canSave)
    }

    @Test
    fun `a valid name can be saved`() {
        createViewModel.onNameChange("Water")

        assertTrue(createViewModel.uiState.value.canSave)
    }

    @Test
    fun `saving creates one tracker with the normalized name and chosen type`() =
        runTest {
            createViewModel.onNameChange("  Walk   dog ")
            createViewModel.onTypeChange(TrackerType.DAYS_SINCE)

            createViewModel.onSave()
            advanceUntilIdle()

            val created = trackers().single()
            assertEquals("Walk dog", created.name)
            assertEquals(TrackerType.DAYS_SINCE, created.type)
            assertEquals(EditorResult.Saved, createViewModel.uiState.value.result)
        }

    @Test
    fun `a start date of today keeps the current time of day`() =
        runTest {
            createViewModel.onNameChange("No sugar")
            createViewModel.onTypeChange(TrackerType.DAYS_SINCE)

            createViewModel.onSave()
            advanceUntilIdle()

            assertEquals(now, trackers().single().startedAt)
        }

    @Test
    fun `an earlier start date begins at midnight in the current zone`() =
        runTest {
            createViewModel.onNameChange("No sugar")
            createViewModel.onTypeChange(TrackerType.DAYS_SINCE)
            createViewModel.onStartDateChange(LocalDate.of(2026, 2, 20))

            createViewModel.onSave()
            advanceUntilIdle()

            assertEquals(warsaw(2026, 2, 20), trackers().single().startedAt)
        }

    @Test
    fun `an earlier start date on a daylight-saving change day begins at that local midnight`() =
        runTest {
            clock.advanceBy(Duration.ofDays(30))
            createViewModel.onNameChange("No sugar")
            createViewModel.onTypeChange(TrackerType.DAYS_SINCE)
            createViewModel.onStartDateChange(LocalDate.of(2026, 3, 29))

            createViewModel.onSave()
            advanceUntilIdle()

            assertEquals(warsaw(2026, 3, 29), trackers().single().startedAt)
        }

    @Test
    fun `a counter ignores the start date and starts now`() =
        runTest {
            createViewModel.onNameChange("Water")
            createViewModel.onStartDateChange(LocalDate.of(2026, 2, 20))

            createViewModel.onSave()
            advanceUntilIdle()

            assertEquals(now, trackers().single().startedAt)
        }

    @Test
    fun `a start date in the future is ignored`() {
        createViewModel.onStartDateChange(today.plusDays(1))

        assertEquals(today, createViewModel.uiState.value.startDate)
    }

    @Test
    fun `tapping save twice creates one tracker`() =
        runTest {
            createViewModel.onNameChange("Water")

            createViewModel.onSave()
            createViewModel.onSave()
            advanceUntilIdle()

            assertEquals(1, trackers().size)
        }

    @Test
    fun `edit mode loads the name and is marked as editing`() =
        runTest {
            val tracker = givenTracker("Water")
            val viewModel = viewModelFor(tracker.id)

            advanceUntilIdle()

            assertEquals("Water", viewModel.uiState.value.name)
            assertTrue(viewModel.uiState.value.isEditing)
        }

    @Test
    fun `saving in edit mode renames the tracker and bumps updatedAt`() =
        runTest {
            val tracker = givenTracker("Water")
            val viewModel = viewModelFor(tracker.id)
            advanceUntilIdle()
            clock.advanceBy(Duration.ofMinutes(5))

            viewModel.onNameChange(" Tea ")
            viewModel.onSave()
            advanceUntilIdle()

            val renamed = trackers().single()
            assertEquals("Tea", renamed.name)
            assertEquals(clock.instant(), renamed.updatedAt)
            assertEquals(EditorResult.Saved, viewModel.uiState.value.result)
        }

    @Test
    fun `an unknown id shows not found and cannot be saved`() =
        runTest {
            val viewModel = viewModelFor("missing")

            advanceUntilIdle()

            assertEquals(EditorLoad.NotFound, viewModel.uiState.value.load)
            assertFalse(viewModel.uiState.value.canSave)
        }

    @Test
    fun `asking to delete shows the confirmation without deleting`() =
        runTest {
            val viewModel = viewModelFor(givenTracker().id)
            advanceUntilIdle()

            viewModel.onDeleteClick()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.showDeleteConfirmation)
            assertEquals(1, trackers().size)
        }

    @Test
    fun `cancelling the confirmation keeps the tracker`() =
        runTest {
            val viewModel = viewModelFor(givenTracker().id)
            advanceUntilIdle()
            viewModel.onDeleteClick()

            viewModel.onDeleteDismiss()
            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.showDeleteConfirmation)
            assertEquals(1, trackers().size)
            assertNull(viewModel.uiState.value.result)
        }

    @Test
    fun `confirming the deletion deletes the tracker and reports it`() =
        runTest {
            val viewModel = viewModelFor(givenTracker().id)
            advanceUntilIdle()
            viewModel.onDeleteClick()

            viewModel.onDeleteConfirm()
            advanceUntilIdle()

            assertTrue(trackers().isEmpty())
            assertEquals(EditorResult.Deleted, viewModel.uiState.value.result)
        }
}
