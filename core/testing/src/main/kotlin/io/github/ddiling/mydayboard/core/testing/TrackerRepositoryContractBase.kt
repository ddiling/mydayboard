package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerEvent
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerType
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Shared fixtures of the repository contract tests. The contract is split by topic into
 * [TrackerContractTest] and [TrackerEventContractTest] (the deletion and no-op topics follow in
 * separate classes); the in-memory fake and the Room implementation each extend all of them, which is what
 * proves they behave identically.
 *
 * The `given...` helpers only set up state, so each test reads as arrange, act, assert.
 */
abstract class TrackerRepositoryContractBase {
    abstract fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository

    /** The clock's reading until a test advances it; also the default tracker start. */
    protected val now: Instant = Instant.parse("2025-03-01T10:00:00Z")
    protected val clock = MutableClock(now, ZoneOffset.UTC)
    protected val repo: TrackerRepository by lazy { createRepository(clock, SequentialIdGenerator()) }

    /** How far a test moves [clock] so that `updatedAt` can differ from `createdAt`. */
    protected val clockAdvance: Duration = Duration.ofMinutes(3)
    protected val nowAfterAdvance: Instant = now.plus(clockAdvance)

    /** A past event may predate its tracker, which an event counter allows. */
    protected val timeBeforeTrackerStart: Instant = now.minus(Duration.ofMinutes(30))
    protected val oldestEventTime: Instant = now.plus(Duration.ofMinutes(1))
    protected val middleEventTime: Instant = now.plus(Duration.ofMinutes(5))
    protected val newestEventTime: Instant = now.plus(Duration.ofMinutes(9))

    /** A corrected time that lands between the middle and the newest event, so sorting must change. */
    protected val correctedEventTime: Instant = now.plus(Duration.ofMinutes(7))

    protected suspend fun givenTracker(
        type: TrackerType = TrackerType.EVENT_COUNTER,
        startedAt: Instant = now,
    ): Tracker = repo.createTracker("Water", type, startedAt)

    protected suspend fun givenEvent(
        tracker: Tracker,
        occurredAt: Instant = oldestEventTime,
    ): TrackerEvent = repo.addEvent(tracker.id, occurredAt) ?: error("Tracker ${tracker.id} should accept events")
}
