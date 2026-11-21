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
import io.github.ddiling.mydayboard.feature.trackers.reason.reasonPrompter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Duration
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class TrackerDetailDaysSinceViewModelTest {
    @get:Rule
    val mainRule = MainDispatcherRule(StandardTestDispatcher())

    private val clock = MutableClock(warsaw(2026, 3, 2, 10), TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val startDate = LocalDate.of(2026, 2, 1)
    private val daysFromStartToNow = 29L

    private fun viewModelFor(tracker: Tracker) =
        TrackerDetailViewModel(
            SavedStateHandle(mapOf("trackerId" to tracker.id)),
            repository,
            clock,
            reasonPrompter(repository, clock),
        )

    private suspend fun givenSugarTracker() =
        repository.createTracker("No sugar", TrackerType.DAYS_SINCE, warsaw(2026, 2, 1))

    private fun TestScope.latest(turbine: ReceiveTurbine<TrackerDetailUiState>): TrackerDetailUiState.DaysSince {
        runCurrent()
        return turbine.expectMostRecentItem() as TrackerDetailUiState.DaysSince
    }

    @Test
    fun `the state shows the days and dates for the clock`() =
        runTest {
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                assertEquals(
                    TrackerDetailUiState.DaysSince(
                        name = "No sugar",
                        currentDays = daysFromStartToNow,
                        currentStart = startDate,
                        longestDays = daysFromStartToNow,
                        brokenStreaks = emptyList<BrokenStreak>(),
                        pendingUndo = null,
                    ),
                    latest(this),
                )
            }
        }

    @Test
    fun `a reset starts a new streak at zero and moves the previous one to the broken list`() =
        runTest {
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                latest(this)
                viewModel.onReset()
                val state = latest(this)

                assertEquals(0L, state.currentDays)
                assertEquals(LocalDate.of(2026, 3, 2), state.currentStart)
                assertEquals(
                    listOf(BrokenStreak(startDate, LocalDate.of(2026, 3, 2), daysFromStartToNow, "id-2")),
                    state.brokenStreaks,
                )
            }
        }

    @Test
    fun `a reset pressed between ticks shows zero days at once`() =
        runTest {
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                latest(this)
                clock.advanceBy(Duration.ofSeconds(30))
                viewModel.onReset()

                assertEquals(0L, latest(this).currentDays)
            }
        }

    @Test
    fun `broken streaks are listed newest first`() =
        runTest {
            val tracker = givenSugarTracker()
            val firstReset = warsaw(2026, 2, 20)
            val secondReset = warsaw(2026, 3, 1)
            repository.addEvent(tracker.id, firstReset)
            repository.addEvent(tracker.id, secondReset)
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                assertEquals(
                    listOf(
                        BrokenStreak(LocalDate.of(2026, 2, 20), LocalDate.of(2026, 3, 1), 9L, "id-3"),
                        BrokenStreak(startDate, LocalDate.of(2026, 2, 20), 19L, "id-2"),
                    ),
                    latest(this).brokenStreaks,
                )
            }
        }

    @Test
    fun `the longest streak updates when the current one beats it`() =
        runTest {
            val tracker = givenSugarTracker()
            repository.addEvent(tracker.id, warsaw(2026, 2, 20))
            val viewModel = viewModelFor(tracker)

            viewModel.uiState.test {
                val before = latest(this)
                clock.advanceBy(Duration.ofDays(10))
                advanceTimeBy(Duration.ofMinutes(1).toMillis())
                val after = latest(this)

                assertEquals(19L, before.longestDays) // the broken streak from Feb 1 to Feb 20
                assertEquals(20L, after.longestDays) // the current streak now beats it
            }
        }

    @Test
    fun `undoing a reset restores the previous streak exactly`() =
        runTest {
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                val before = latest(this)
                viewModel.onReset()
                latest(this)
                viewModel.onUndo()
                val after = latest(this)

                assertEquals(before, after)
            }
        }

    @Test
    fun `a tick after midnight adds a day`() =
        runTest {
            clock.now = warsaw(2026, 3, 2, 23, 59).plusSeconds(30)
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                val before = latest(this)
                clock.now = warsaw(2026, 3, 3)
                advanceTimeBy(Duration.ofSeconds(31).toMillis())
                val after = latest(this)

                assertEquals(before.currentDays + 1, after.currentDays)
            }
        }

    @Test
    fun `a reset registers a reset undo`() =
        runTest {
            val viewModel = viewModelFor(givenSugarTracker())

            viewModel.uiState.test {
                latest(this)
                viewModel.onReset()
                val undo = latest(this).pendingUndo

                assertNotNull(undo)
                assertEquals(UndoKind.RESET, undo?.kind)
            }
        }
}
