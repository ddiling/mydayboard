package io.github.ddiling.mydayboard.feature.trackers.list

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.domain.tracker.ObserveTrackerSummariesUseCase
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MainDispatcherRule
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.time.Duration

/**
 * The use case is built with its public constructor (its test-ticker constructor is internal to `:core:domain`),
 * so time moves by setting the [MutableClock] and advancing virtual time to the next minute tick. A standard
 * dispatcher keeps the ViewModel's work queued, so each step is run explicitly.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrackersListViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val now = warsaw(2026, 3, 2, 10)
    private val fiveMinutes = Duration.ofMinutes(5)
    private val clock = MutableClock(now, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())

    // Lazy: viewModelScope must be created after the rule has installed the test Main dispatcher.
    private val viewModel by lazy { TrackersListViewModel(ObserveTrackerSummariesUseCase(repository, clock)) }

    @Test
    fun `the state is loading until the first summaries arrive and then empty`() =
        runTest {
            viewModel.uiState.test {
                assertEquals(TrackersListUiState.Loading, awaitItem())
                runCurrent()
                assertEquals(TrackersListUiState.Empty, awaitItem())
            }
        }

    @Test
    fun `content lists both tracker types in repository order`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)
            val sugar = repository.createTracker("No sugar", TrackerType.DAYS_SINCE, warsaw(2026, 2, 27, 8))
            repository.addEvent(water.id, now - fiveMinutes)

            viewModel.uiState.test {
                runCurrent()
                skipItems(1)
                val counter = TrackerListItem.Counter(water.id, "Water", 1, sinceLast = fiveMinutes)
                val daysSince = TrackerListItem.DaysSince(sugar.id, "No sugar", days = 3)
                assertEquals(TrackersListUiState.Content(listOf(counter, daysSince)), awaitItem())
            }
        }

    @Test
    fun `content updates when the repository changes`() =
        runTest {
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, now)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                repository.addEvent(water.id, now)
                runCurrent()

                val withEvent = TrackerListItem.Counter(water.id, "Water", todayCount = 1, sinceLast = Duration.ZERO)
                assertEquals(TrackersListUiState.Content(listOf(withEvent)), awaitItem())
            }
        }

    @Test
    fun `a tick after midnight resets today's count`() =
        runTest {
            val lateEvening = warsaw(2026, 3, 2, 23, 59)
            clock.now = lateEvening.plusSeconds(30)
            val water = repository.createTracker("Water", TrackerType.EVENT_COUNTER, lateEvening)
            repository.addEvent(water.id, lateEvening)

            viewModel.uiState.test {
                runCurrent()
                skipItems(2)

                clock.now = warsaw(2026, 3, 3)
                advanceTimeBy(Duration.ofSeconds(31).toMillis())
                runCurrent()

                assertEquals(0, (awaitItem() as TrackersListUiState.Content).items.counterAt(0).todayCount)
            }
        }

    private fun List<TrackerListItem>.counterAt(index: Int) = get(index) as TrackerListItem.Counter
}
