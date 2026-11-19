package io.github.ddiling.mydayboard.core.domain.tracker

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.testing.FakeTrackerRepository
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant

class ObserveTrackerSummariesUseCaseTest {
    private val startOfDay = warsaw(2026, 3, 10, 12)
    private val clock = MutableClock(startOfDay, TestZones.WARSAW)
    private val repository = FakeTrackerRepository(clock, SequentialIdGenerator())
    private val ticks = MutableSharedFlow<Instant>()
    private val useCase = ObserveTrackerSummariesUseCase(repository, clock) { ticks }

    private suspend fun tick() = ticks.emit(clock.instant())

    @Test
    fun `an empty repository gives an empty list`() =
        runTest {
            useCase().test {
                tick()
                assertEquals(emptyList<TrackerSummary>(), awaitItem())
            }
        }

    @Test
    fun `mixed types give the matching subtype and summary per tracker in tracker order`() =
        runTest {
            val counter = repository.createTracker("Water", TrackerType.EVENT_COUNTER, startOfDay)
            clock.advanceBy(Duration.ofSeconds(1))
            val daysSince = repository.createTracker("Smoke free", TrackerType.DAYS_SINCE, warsaw(2026, 3, 7, 12))
            repository.addEvent(counter.id, startOfDay)

            useCase().test {
                tick()
                val items = awaitItem()
                assertEquals(listOf(counter, daysSince), items.map { it.tracker })
                val counterItem = items[0] as TrackerSummary.Counter
                val daysItem = items[1] as TrackerSummary.DaysSince
                assertEquals(1, counterItem.summary.todayCount)
                assertEquals(3L, daysItem.summary.currentDays)
            }
        }

    @Test
    fun `events of one tracker do not leak into another`() =
        runTest {
            val first = repository.createTracker("First", TrackerType.EVENT_COUNTER, startOfDay)
            repository.createTracker("Second", TrackerType.EVENT_COUNTER, startOfDay)
            repository.addEvent(first.id, startOfDay)
            repository.addEvent(first.id, startOfDay)

            useCase().test {
                tick()
                val counts = awaitItem().map { (it as TrackerSummary.Counter).summary.todayCount }
                assertEquals(listOf(2, 0), counts)
            }
        }

    @Test
    fun `a tick after midnight resets today's count without repository changes`() =
        runTest {
            val tracker = repository.createTracker("Water", TrackerType.EVENT_COUNTER, startOfDay)
            repository.addEvent(tracker.id, startOfDay)

            useCase().test {
                tick()
                assertEquals(1, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)

                clock.now = warsaw(2026, 3, 11, 0, 1)
                tick()
                assertEquals(0, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)
            }
        }

    @Test
    fun `a zone change on the clock is picked up at the next tick`() =
        runTest {
            val tracker = repository.createTracker("Water", TrackerType.EVENT_COUNTER, startOfDay)
            // 12:00 in Warsaw is 20:00 in Tokyo the same day; 23:00 Warsaw is already the next day in Tokyo.
            repository.addEvent(tracker.id, startOfDay)
            clock.now = warsaw(2026, 3, 10, 23)

            useCase().test {
                tick()
                assertEquals(1, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)

                clock.zone = TestZones.TOKYO
                tick()
                assertEquals(0, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)
            }
        }

    @Test
    fun `adding an event emits a new list`() =
        runTest {
            val tracker = repository.createTracker("Water", TrackerType.EVENT_COUNTER, startOfDay)

            useCase().test {
                tick()
                assertEquals(0, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)

                repository.addEvent(tracker.id, startOfDay)
                assertEquals(1, (awaitItem().single() as TrackerSummary.Counter).summary.todayCount)
            }
        }

    @Test
    fun `an event added between ticks appears without waiting for the next tick`() =
        runTest {
            val tracker = repository.createTracker("Water", TrackerType.EVENT_COUNTER, startOfDay)

            useCase().test {
                tick()
                awaitItem()

                clock.advanceBy(Duration.ofSeconds(30))
                val eventTimeAfterLastTick = clock.instant()
                repository.addEvent(tracker.id, eventTimeAfterLastTick)

                val summary = (awaitItem().single() as TrackerSummary.Counter).summary
                assertEquals(1, summary.todayCount)
                assertEquals(eventTimeAfterLastTick, summary.lastEventAt)
            }
        }

    @Test
    fun `a days since tracker restarts from its reset event`() =
        runTest {
            val tracker = repository.createTracker("Smoke free", TrackerType.DAYS_SINCE, warsaw(2026, 3, 7, 12))
            val resetTime = warsaw(2026, 3, 9, 12)
            repository.addEvent(tracker.id, resetTime)

            useCase().test {
                tick()
                val summary = (awaitItem().single() as TrackerSummary.DaysSince).summary
                assertEquals(resetTime, summary.currentStart)
                assertEquals(1L, summary.currentDays)
            }
        }
}
