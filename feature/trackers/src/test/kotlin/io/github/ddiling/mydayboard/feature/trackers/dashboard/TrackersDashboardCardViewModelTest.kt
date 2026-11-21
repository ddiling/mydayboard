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
import kotlinx.coroutines.test.TestScope
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
    private val gapSinceLastEvent = Duration.ofMinutes(5)
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
            repository.addEvent(water.id, now - gapSinceLastEvent)

            viewModel.uiState.test {
                assertEquals(TrackersDashboardCardUiState.Loading, awaitItem())
                runCurrent()
                val counter =
                    TrackersDashboardRow.Counter(
                        water.id,
                        "Water",
                        todayCount = 1,
                        sinceLast = gapSinceLastEvent,
                    )
                val daysSince = TrackersDashboardRow.DaysSince(sugar.id, "No sugar", days = 3)
                val expected = TrackersDashboardCardUiState.Content(listOf(counter, daysSince), 2, pendingUndo = null)
                assertEquals(expected, awaitItem())
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

    @Test
    fun `with 6 trackers the grid shows the first 4 in list order`() =
        runTest {
            val ids = (1..6).map { repository.createTracker("Tracker $it", TrackerType.EVENT_COUNTER, now).id }

            assertEquals(ids.take(4), currentContent().rows.map { it.id })
        }

    @Test
    fun `total count shows all 6 trackers, not 4`() =
        runTest {
            repeat(6) { repository.createTracker("Tracker $it", TrackerType.EVENT_COUNTER, now) }

            assertEquals(6, currentContent().totalCount)
        }

    @Test
    fun `undo restores the previous value and ends the offer`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)
                viewModel.onPlusOne(water.id)
                runCurrent()
                skipItems(1) // the row with the new event
                viewModel.onUndo()
                runCurrent()

                val content = expectMostRecentItem() as TrackersDashboardCardUiState.Content
                assertEquals(0, (content.rows.single() as TrackersDashboardRow.Counter).todayCount)
                assertEquals(null, content.pendingUndo)
            }
        }

    @Test
    fun `counter today count goes back to 0 at midnight`() =
        runTest {
            assertEquals(0, counterRowAfterMidnight().todayCount)
        }

    @Test
    fun `counter time since the last event keeps counting across midnight`() =
        runTest {
            assertEquals(Duration.ofMinutes(1), counterRowAfterMidnight().sinceLast)
        }

    /** Logs one event at 23:59, moves the clock to 00:00 the next day and returns the row after the minute tick. */
    private suspend fun TestScope.counterRowAfterMidnight(): TrackersDashboardRow.Counter {
        val beforeMidnight = warsaw(2026, 3, 2, 23, 59)
        clock.now = beforeMidnight
        val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, beforeMidnight)
        repository.addEvent(water.id, beforeMidnight)
        lateinit var row: TrackersDashboardRow.Counter
        viewModel.uiState.test {
            runCurrent()
            skipItems(2)
            clock.now = warsaw(2026, 3, 3, 0, 0)
            advanceTimeBy(Duration.ofSeconds(61).toMillis())
            runCurrent()
            row = (awaitItem() as TrackersDashboardCardUiState.Content).rows.single() as TrackersDashboardRow.Counter
        }
        return row
    }

    @Test
    fun `days since counts a 23-hour daylight-saving day as one day`() =
        runTest {
            val dayBeforeSpringForward = warsaw(2026, 3, 28, 12)
            clock.now = warsaw(2026, 3, 29, 12)
            repository.createTracker("No sugar", TrackerType.DAYS_SINCE, dayBeforeSpringForward)

            assertEquals(1, (currentContent().rows.single() as TrackersDashboardRow.DaysSince).days)
        }

    @Test
    fun `days since follows the zone of the injected clock`() =
        runTest {
            val lateEveningInWarsaw = warsaw(2026, 3, 2, 22)
            clock.now = warsaw(2026, 3, 3, 12)
            clock.zone = TestZones.TOKYO
            repository.createTracker("No sugar", TrackerType.DAYS_SINCE, lateEveningInWarsaw)

            // In Tokyo the tracker started on the 3rd, the same day as now.
            assertEquals(0, (currentContent().rows.single() as TrackersDashboardRow.DaysSince).days)
        }

    private suspend fun TestScope.currentContent(): TrackersDashboardCardUiState.Content {
        var content: TrackersDashboardCardUiState.Content? = null
        viewModel.uiState.test {
            runCurrent()
            content = expectMostRecentItem() as TrackersDashboardCardUiState.Content
            cancelAndIgnoreRemainingEvents()
        }
        return checkNotNull(content)
    }

    private suspend fun eventTimes(trackerId: String) =
        repository.observeEvents(trackerId).first().map { it.occurredAt }
}
