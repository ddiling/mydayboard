package io.github.ddiling.mydayboard.core.domain.time

import app.cash.turbine.test
import io.github.ddiling.mydayboard.core.testing.MutableClock
import io.github.ddiling.mydayboard.core.testing.TestZones
import io.github.ddiling.mydayboard.core.testing.warsaw
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.Instant
import kotlin.time.toKotlinDuration

class MinuteTickerTest {
    private val start = warsaw(2026, 3, 2, 10, 0).plusSeconds(30)
    private val clock = MutableClock(start, TestZones.WARSAW)

    /**
     * Moves the clock and virtual time together, as real time moves both, and runs what became due. A step must
     * not pass a tick: the ticker reads the clock only when it wakes. Stop 1 ms short of a tick before asserting
     * silence: the clock moves before virtual time, so a tick up to the step early would otherwise go unseen.
     * Tests then use `expectMostRecentItem()`, which fails at once if nothing came; `awaitItem()` would let
     * runTest skip ahead and hide a late tick.
     */
    private fun TestScope.passTime(duration: Duration) {
        clock.advanceBy(duration)
        testScheduler.advanceTimeBy(duration.toKotlinDuration())
        testScheduler.runCurrent()
    }

    @Test
    fun `first instant is emitted immediately`() =
        runTest {
            minuteTicker(clock).test {
                assertEquals(start, expectMostRecentItem())
            }
        }

    @Test
    fun `second tick comes at the start of the next minute`() =
        runTest {
            minuteTicker(clock).test {
                expectMostRecentItem()

                passTime(Duration.ofMillis(29_999))
                expectNoEvents()
                passTime(Duration.ofMillis(1))

                assertEquals(warsaw(2026, 3, 2, 10, 1), expectMostRecentItem())
            }
        }

    @Test
    fun `later ticks stay on the full minute when time passes between ticks`() =
        runTest {
            // Each emission takes 5 s to handle (or the device woke late), so the clock runs ahead of the delays.
            minuteTicker(clock).onEach { clock.advanceBy(Duration.ofSeconds(5)) }.test {
                expectMostRecentItem()
                passTime(Duration.ofSeconds(25))
                assertEquals(warsaw(2026, 3, 2, 10, 1), expectMostRecentItem())

                passTime(Duration.ofMillis(54_999))
                expectNoEvents()
                passTime(Duration.ofMillis(1))

                assertEquals(warsaw(2026, 3, 2, 10, 2), expectMostRecentItem())
            }
        }

    @Test
    fun `cancelling the collector stops the ticks`() =
        runTest {
            val emitted = mutableListOf<Instant>()
            val collector = launch { minuteTicker(clock).toList(emitted) }
            testScheduler.runCurrent()

            collector.cancel()
            passTime(Duration.ofSeconds(30))
            passTime(Duration.ofSeconds(60))

            assertEquals(listOf(start), emitted)
        }
}
