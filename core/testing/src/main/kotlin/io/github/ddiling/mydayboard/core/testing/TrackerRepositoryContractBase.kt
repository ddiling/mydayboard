package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

/**
 * Shared fixtures of the repository contract tests. The contract is split by topic into
 * [TrackerContractTest], [TrackerEventContractTest] and [TrackerNoOpContractTest]; the in-memory fake and the
 * Room implementation each extend all three, which is what proves they behave identically.
 *
 * The contract lives in main sources, so it follows the production code style: behaviour names are camelCase
 * and instants are named properties instead of inline numbers.
 */
abstract class TrackerRepositoryContractBase {
    abstract fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository

    protected val start: Instant = Instant.parse("2025-03-01T10:00:00Z")
    protected val clock = MutableClock(start, ZoneOffset.UTC)
    protected val repo: TrackerRepository by lazy { createRepository(clock, SequentialIdGenerator()) }

    /** How far [clock] is moved when a test needs `updatedAt` to differ from `createdAt`. */
    protected val step: Duration = Duration.ofMinutes(3)
    protected val afterStep: Instant = start.plus(step)

    protected val earlier: Instant = start.minus(Duration.ofMinutes(30))
    protected val minute1: Instant = start.plus(Duration.ofMinutes(1))
    protected val minute2: Instant = start.plus(Duration.ofMinutes(2))
    protected val minute5: Instant = start.plus(Duration.ofMinutes(5))
    protected val minute8: Instant = start.plus(Duration.ofMinutes(8))
    protected val minute9: Instant = start.plus(Duration.ofMinutes(9))
}
