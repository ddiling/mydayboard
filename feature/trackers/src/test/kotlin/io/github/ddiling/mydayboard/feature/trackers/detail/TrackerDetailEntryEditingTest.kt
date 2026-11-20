package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime

/** Entry editing through the ViewModel: what the screen sees after add, edit and delete-with-undo. */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerDetailEntryEditingTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = MutableClock(warsaw(2026, 3, 2, 10), TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val earlyEvent = warsaw(2026, 3, 1, 8)
    private val lateEvent = warsaw(2026, 3, 1, 20)

    private fun viewModelFor(tracker: Tracker) =
        TrackerDetailViewModel(SavedStateHandle(mapOf("trackerId" to tracker.id)), repository, clock)

    private suspend fun givenCounter() =
        repository.createTracker("Water", TrackerType.EVENT_COUNTER, warsaw(2026, 2, 1))

    private fun TestScope.counter(turbine: ReceiveTurbine<TrackerDetailUiState>): TrackerDetailUiState.Counter {
        runCurrent()
        return turbine.expectMostRecentItem() as TrackerDetailUiState.Counter
    }

    private fun HistoryDay.ids() = entries.map { it.eventId }

    @Test
    fun `editing an entry to a later time re-sorts the history`() =
        runTest {
            val tracker = givenCounter()
            val early = repository.addEvent(tracker.id, earlyEvent)!!
            val late = repository.addEvent(tracker.id, lateEvent)!!
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                counter(this)
                viewModel.entryEditor.openEdit(early.id)
                counter(this)
                viewModel.entryEditor.changeTime(LocalTime.of(21, 0))
                viewModel.entryEditor.save()
                val history = counter(this).history

                assertEquals(listOf(listOf(early.id, late.id)), history.map { it.ids() })
            }
        }

    @Test
    fun `an open dialog is part of the state and closes after saving`() =
        runTest {
            val tracker = givenCounter()
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                counter(this)
                viewModel.entryEditor.openAdd()
                val opened = counter(this).editor
                viewModel.entryEditor.save()
                val closed = counter(this).editor

                assertEquals(LocalDate.of(2026, 3, 2), opened?.date)
                assertEquals(null, closed)
            }
        }

    @Test
    fun `deleting an entry offers an undo that restores it`() =
        runTest {
            val tracker = givenCounter()
            val event = repository.addEvent(tracker.id, earlyEvent)!!
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                counter(this)
                viewModel.entryEditor.openEdit(event.id)
                counter(this)
                viewModel.entryEditor.delete()
                val afterDelete = counter(this)
                viewModel.onUndo()
                val afterUndo = counter(this)

                assertEquals(Undo(event.id, UndoKind.DELETED), afterDelete.pendingUndo)
                assertEquals(emptyList<HistoryDay>(), afterDelete.history)
                assertEquals(listOf(listOf(event.id)), afterUndo.history.map { it.ids() })
            }
        }

    @Test
    fun `broken streaks name the reset that ended them, ignoring resets before the start`() =
        runTest {
            val tracker = repository.createTracker("No sugar", TrackerType.DAYS_SINCE, warsaw(2026, 2, 1))
            repository.addEvent(tracker.id, warsaw(2026, 1, 15))
            val first = repository.addEvent(tracker.id, warsaw(2026, 2, 20))!!
            val second = repository.addEvent(tracker.id, warsaw(2026, 3, 1))!!
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                runCurrent()
                val state = expectMostRecentItem() as TrackerDetailUiState.DaysSince

                assertEquals(listOf(second.id, first.id), state.brokenStreaks.map { it.endedByEventId })
            }
        }
}
