package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.DayCount
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.domain.tracker.Trend
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

/**
 * Robolectric supplies the real `Bundle` that `toRoute` needs to read the route arguments. Time moves by setting
 * the [MutableClock] and advancing virtual time to the next minute tick.
 */
@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerDetailViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10)
    private val today = LocalDate.of(2026, 3, 2)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val startedLongAgo = warsaw(2026, 2, 1)

    private fun viewModelFor(trackerId: String) =
        TrackerDetailViewModel(SavedStateHandle(mapOf("trackerId" to trackerId)), repository, clock)

    private suspend fun givenCounter(startedAt: Instant = startedLongAgo) =
        repository.createTracker("Water", TrackerType.EVENT_COUNTER, startedAt)

    private suspend fun TestScope.trendWith(
        startedAt: Instant,
        eventAt: Instant,
    ): Trend? {
        val water = givenCounter(startedAt)
        repository.addEvent(water.id, eventAt)
        var trend: Trend? = null
        viewModelFor(water.id).uiState.test {
            runCurrent()
            skipItems(1)
            trend = (awaitItem() as TrackerDetailUiState.Counter).trend
        }
        return trend
    }

    private suspend fun TestScope.givenPlusOnePressed(
        turbine: ReceiveTurbine<TrackerDetailUiState>,
        viewModel: TrackerDetailViewModel,
    ) {
        runCurrent()
        turbine.skipItems(2)
        viewModel.onPlusOne()
        runCurrent()
        turbine.skipItems(1)
    }

    @Test
    fun `an unknown tracker is not found`() =
        runTest {
            val viewModel = viewModelFor("missing")

            viewModel.uiState.test {
                assertEquals(TrackerDetailUiState.Loading, awaitItem())
                runCurrent()
                assertEquals(TrackerDetailUiState.NotFound, awaitItem())
            }
        }

    @Test
    fun `counter content shows today, time since last, the last week and the history`() =
        runTest {
            val water = givenCounter()
            repository.addEvent(water.id, warsaw(2026, 3, 1, 20))
            repository.addEvent(water.id, warsaw(2026, 3, 2, 9, 30))
            repository.addEvent(water.id, warsaw(2026, 3, 2, 8))
            val viewModel = viewModelFor(water.id)

            viewModel.uiState.test {
                runCurrent()
                skipItems(1)
                val counter = awaitItem() as TrackerDetailUiState.Counter

                assertEquals("Water", counter.name)
                assertEquals(2, counter.todayCount)
                assertEquals(Duration.ofMinutes(30), counter.sinceLast)
                assertEquals(7, counter.lastDays.size)
                assertEquals(listOf(DayCount(today.minusDays(1), 1), DayCount(today, 2)), counter.lastDays.takeLast(2))
                assertEquals(
                    listOf(
                        HistoryDay(today, listOf(LocalTime.of(9, 30), LocalTime.of(8, 0))),
                        HistoryDay(today.minusDays(1), listOf(LocalTime.of(20, 0))),
                    ),
                    counter.history,
                )
            }
        }

    @Test
    fun `an event backdated before the start date extends the history used for the trend`() =
        runTest {
            val trend = trendWith(startedAt = now, eventAt = warsaw(2026, 2, 10, 12))

            assertEquals(Trend.FLAT, trend)
        }

    @Test
    fun `an old start date gives a recent first event enough history for a trend`() =
        runTest {
            val trend = trendWith(startedAt = warsaw(2026, 2, 10), eventAt = warsaw(2026, 3, 2, 9))

            assertEquals(Trend.FLAT, trend)
        }

    @Test
    fun `plus one adds an event at the clock time and the state updates`() =
        runTest {
            val water = givenCounter()
            val viewModel = viewModelFor(water.id)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                viewModel.onPlusOne()
                runCurrent()

                val counter = awaitItem() as TrackerDetailUiState.Counter
                assertEquals(1, counter.todayCount)
                assertEquals(listOf(now), repository.observeEvents(water.id).first().map { it.occurredAt })
            }
        }

    @Test
    fun `plus one makes the new event undoable`() =
        runTest {
            val water = givenCounter()
            val viewModel = viewModelFor(water.id)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                viewModel.onPlusOne()
                runCurrent()

                val eventId =
                    repository
                        .observeEvents(water.id)
                        .first()
                        .single()
                        .id
                assertEquals(
                    Undo(eventId, UndoKind.ADDED),
                    (expectMostRecentItem() as TrackerDetailUiState.Counter).pendingUndo,
                )
            }
        }

    @Test
    fun `undo removes the added event and the count goes back`() =
        runTest {
            val viewModel = viewModelFor(givenCounter().id)

            viewModel.uiState.test {
                givenPlusOnePressed(this, viewModel)

                viewModel.onUndo()
                runCurrent()

                val counter = expectMostRecentItem() as TrackerDetailUiState.Counter
                assertEquals(0, counter.todayCount)
                assertNull(counter.pendingUndo)
            }
        }

    @Test
    fun `dismissing the undo keeps the event`() =
        runTest {
            val viewModel = viewModelFor(givenCounter().id)

            viewModel.uiState.test {
                givenPlusOnePressed(this, viewModel)

                viewModel.onUndoDismissed()
                runCurrent()

                val counter = awaitItem() as TrackerDetailUiState.Counter
                assertNull(counter.pendingUndo)
                assertEquals(1, counter.todayCount)
            }
        }

    @Test
    fun `a tick after midnight resets today's count`() =
        runTest {
            val lateEvening = warsaw(2026, 3, 2, 23, 59)
            clock.now = lateEvening.plusSeconds(30)
            val water = givenCounter()
            repository.addEvent(water.id, lateEvening)
            val viewModel = viewModelFor(water.id)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                clock.now = warsaw(2026, 3, 3)
                advanceTimeBy(Duration.ofSeconds(31).toMillis())
                runCurrent()

                assertEquals(0, (awaitItem() as TrackerDetailUiState.Counter).todayCount)
            }
        }

    @Test
    fun `deleting the tracker elsewhere shows not found`() =
        runTest {
            val water = givenCounter()
            val viewModel = viewModelFor(water.id)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                repository.deleteTracker(water.id)
                runCurrent()

                assertEquals(TrackerDetailUiState.NotFound, expectMostRecentItem())
            }
        }
}
