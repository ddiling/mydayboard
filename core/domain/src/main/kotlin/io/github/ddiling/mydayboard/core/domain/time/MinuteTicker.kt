package io.github.ddiling.mydayboard.core.domain.time

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.toKotlinDuration

/**
 * Emits the current instant right away, then at the start of every following minute, until cancelled.
 *
 * Screens that show "today" or "time since" combine their data with this flow, so the dashboard rolls over
 * at midnight without a restart. Ticking on the full minute (not every 60 s from whenever collection started)
 * keeps the screen in step with the status bar clock. The wait is recomputed from [clock] before every tick,
 * so a late wake-up (the device slept) does not shift later ticks. Full minutes are taken on the UTC [Instant];
 * they are local full minutes too, as every zone in use today has a whole-minute offset.
 *
 * No dispatcher is chosen here: the delay runs on the collector's, which lets tests use virtual time.
 */
fun minuteTicker(clock: Clock): Flow<Instant> =
    flow {
        while (true) {
            val now = clock.instant()
            emit(now)
            // delay(kotlin.time.Duration) rounds up to whole milliseconds, so it never wakes before the minute.
            delay(timeUntilNextMinute(now).toKotlinDuration())
        }
    }

private fun timeUntilNextMinute(now: Instant): Duration {
    val nextMinute = now.truncatedTo(ChronoUnit.MINUTES).plus(1, ChronoUnit.MINUTES)
    return Duration.between(now, nextMinute)
}
