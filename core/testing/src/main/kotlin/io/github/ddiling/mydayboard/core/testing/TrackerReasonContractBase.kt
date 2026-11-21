package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.Tracker
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository

/**
 * Fixtures of the reason contract tests. The tracker repository and the reason repository must see the same
 * trackers, so an implementation builds both over one store and shares [ids] with them.
 */
abstract class TrackerReasonContractBase : TrackerRepositoryContractBase() {
    abstract fun createReasonRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerReasonRepository

    protected val reasons: TrackerReasonRepository by lazy { createReasonRepository(clock, ids) }

    protected suspend fun givenReason(
        tracker: Tracker,
        text: String = "Stay hydrated",
    ): TrackerReason = reasons.addReason(tracker.id, text) ?: error("Tracker ${tracker.id} should accept reasons")

    protected suspend fun givenDeletedReason(tracker: Tracker): TrackerReason =
        givenReason(tracker).also { reasons.deleteReason(it.id) }
}
