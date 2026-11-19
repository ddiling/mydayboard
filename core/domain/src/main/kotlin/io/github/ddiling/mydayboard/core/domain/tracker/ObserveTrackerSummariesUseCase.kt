package io.github.ddiling.mydayboard.core.domain.tracker

import io.github.ddiling.mydayboard.core.domain.time.minuteTicker
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.Clock
import java.time.Instant
import javax.inject.Inject

/** What the tracker list and the dashboard show for one tracker; the subtype follows the tracker's type. */
sealed interface TrackerSummary {
    val tracker: Tracker

    data class Counter(
        override val tracker: Tracker,
        val summary: CounterSummary,
    ) : TrackerSummary

    data class DaysSince(
        override val tracker: Tracker,
        val summary: DaysSinceSummary,
    ) : TrackerSummary
}

/**
 * The one place that turns trackers, events and time into display summaries, so the list and the dashboard
 * cannot disagree. It recomputes on every repository change and on every [ticker] emission, which lets
 * "today" and "days since" roll over at midnight without any data change. The zone is read from
 * [Clock.getZone] at each emission, so a time zone change is picked up at the next tick.
 *
 * The ticker is only a trigger: "now" is always read from [clock] at the emission, never taken from the ticker's
 * value, which can be up to a minute old when a repository change causes the recompute. Otherwise an event
 * added just after a tick would count as "in the future" and stay hidden until the next tick.
 *
 * The ticker is a constructor parameter so tests can drive time themselves. Hilt cannot use a Kotlin default
 * argument, so the production default lives in a separate `@Inject` constructor instead.
 */
class ObserveTrackerSummariesUseCase internal constructor(
    private val repository: TrackerRepository,
    private val clock: Clock,
    private val ticker: (Clock) -> Flow<Instant>,
) {
    @Inject
    constructor(repository: TrackerRepository, clock: Clock) : this(repository, clock, ::minuteTicker)

    operator fun invoke(): Flow<List<TrackerSummary>> =
        combine(repository.observeTrackers(), repository.observeAllEvents(), ticker(clock)) { trackers, events, _ ->
            val now = clock.instant()
            val zone = clock.zone
            val eventTimesByTracker = events.groupBy({ it.trackerId }, { it.occurredAt })
            trackers.map { tracker ->
                val eventTimes = eventTimesByTracker[tracker.id].orEmpty()
                when (tracker.type) {
                    TrackerType.EVENT_COUNTER -> {
                        TrackerSummary.Counter(tracker, summarizeCounter(eventTimes, now, zone))
                    }

                    TrackerType.DAYS_SINCE -> {
                        TrackerSummary.DaysSince(tracker, summarizeDaysSince(tracker.startedAt, eventTimes, now, zone))
                    }
                }
            }
        }
}
