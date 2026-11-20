package io.github.ddiling.mydayboard.feature.trackers.dashboard

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.ObserveTrackerSummariesUseCase
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import io.github.ddiling.mydayboard.feature.trackers.detail.UndoKind
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Duration
import java.time.Instant

/**
 * The use case is built with its public constructor (its test-ticker constructor is internal to `:core:domain`),
 * so time moves by setting the [MutableClock] and advancing virtual time to the next minute tick.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackersDashboardCardViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10)
    private val fiveMinutes = Duration.ofMinutes(5)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private val viewModel by lazy {
        TrackersDashboardCardViewModel(ObserveTrackerSummariesUseCase(repository, clock), repository, clock)
    }

    @Test
    fun `rows show both tracker types`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            val sugar = repository.createTracker("No sugar", TrackerType.DAYS_SINCE, warsaw(2026, 2, 27, 8))
            repository.addEvent(water.id, now - fiveMinutes)

            viewModel.uiState.test {
                assertEquals(TrackersDashboardCardUiState.Loading, awaitItem())
                runCurrent()
                val counter = TrackersDashboardRow.Counter(water.id, "Water", todayCount = 1, sinceLast = fiveMinutes)
                val daysSince = TrackersDashboardRow.DaysSince(sugar.id, "No sugar", days = 3)
                assertEquals(TrackersDashboardCardUiState.Content(listOf(counter, daysSince), null), awaitItem())
            }
        }

    @Test
    fun `plus one adds an event for that tracker at the clock time`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            val tea = repository.createTracker("Tea", TrackerType.EVENT_COUNTER, now)

            viewModel.onPlusOne(water.id)
            runCurrent()

            assertEquals(listOf(now), eventTimes(water.id))
            assertEquals(emptyList<TrackerEvent>(), repository.observeEvents(tea.id).first())
        }

    @Test
    fun `reset adds an event for that tracker at the clock time and offers a reset undo`() =
        runTest {
            val sugar = repository.createTracker("No sugar", TrackerType.DAYS_SINCE, warsaw(2026, 2, 27, 8))

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)
                viewModel.onReset(sugar.id)
                runCurrent()

                assertEquals(listOf(now), eventTimes(sugar.id))
                val pending = (awaitItem() as TrackersDashboardCardUiState.Content).pendingUndo
                assertEquals(UndoKind.RESET, pending?.kind)
            }
        }

    @Test
    fun `undo removes only the event of the last action`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            viewModel.onPlusOne(water.id)
            runCurrent()
            clock.now = now.plusSeconds(10)
            viewModel.onPlusOne(water.id)
            runCurrent()

            viewModel.onUndo()
            runCurrent()

            assertEquals(listOf(now), eventTimes(water.id))
        }

    @Test
    fun `undo after a second action on another tracker removes only the second event`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            val tea = repository.createTracker("Tea", TrackerType.EVENT_COUNTER, now)
            viewModel.onPlusOne(water.id)
            runCurrent()
            viewModel.onPlusOne(tea.id)
            runCurrent()

            viewModel.onUndo()
            runCurrent()
            viewModel.onUndo()
            runCurrent()

            assertEquals(listOf(now), eventTimes(water.id))
            assertEquals(emptyList<Instant>(), eventTimes(tea.id))
        }

    @Test
    fun `a minute tick updates since last`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            repository.addEvent(water.id, now)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                clock.now = now.plusSeconds(60)
                advanceTimeBy(Duration.ofSeconds(61).toMillis())
                runCurrent()

                val row = (awaitItem() as TrackersDashboardCardUiState.Content).rows.single()
                assertEquals(Duration.ofMinutes(1), (row as TrackersDashboardRow.Counter).sinceLast)
            }
        }

    @Test
    fun `an action thirty seconds after the tick shows at once`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                clock.now = now.plusSeconds(30)
                viewModel.onPlusOne(water.id)
                runCurrent()

                val row = (expectMostRecentItem() as TrackersDashboardCardUiState.Content).rows.single()
                assertEquals(1, (row as TrackersDashboardRow.Counter).todayCount)
            }
        }

    private suspend fun eventTimes(trackerId: String) =
        repository.observeEvents(trackerId).first().map { it.occurredAt }
}
