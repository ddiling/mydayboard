package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReasonRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.time.Clock

/**
 * In-memory [TrackerReasonRepository] for ViewModel tests. It passes the same contract tests as the Room
 * implementation. Pass the [FakeTrackerStore] of the [FakeTrackerRepository] to use the same trackers. Deleted
 * reasons are kept aside only so that [restoreReason] can bring them back.
 */
class FakeTrackerReasonRepository(
    private val clock: Clock,
    private val ids: IdGenerator,
    store: FakeTrackerStore = FakeTrackerStore(),
) : TrackerReasonRepository {
    private val state = store.state

    private val reasonOrder = compareBy<TrackerReason> { it.createdAt }.thenBy { it.id }

    override fun observeReasons(trackerId: String): Flow<List<TrackerReason>> =
        state
            .map { s -> s.reasons.filter { it.trackerId == trackerId }.sortedWith(reasonOrder) }
            .distinctUntilChanged()

    override suspend fun setAskForReason(
        id: String,
        enabled: Boolean,
    ) {
        val now = clock.instant()
        state.update { s ->
            s.copy(
                trackers =
                    s.trackers.map {
                        if (it.id ==
                            id
                        ) {
                            it.copy(askForReason = enabled, updatedAt = now)
                        } else {
                            it
                        }
                    },
            )
        }
    }

    override suspend fun addReason(
        trackerId: String,
        text: String,
    ): TrackerReason? {
        val now = clock.instant()
        val reason = TrackerReason(ids.newId(), trackerId, text, createdAt = now, updatedAt = now)
        val before =
            state.getAndUpdate { s ->
                if (s.hasTracker(trackerId)) s.copy(reasons = s.reasons + reason) else s
            }
        return reason.takeIf { before.hasTracker(trackerId) }
    }

    override suspend fun updateReason(
        reasonId: String,
        text: String,
    ) {
        val now = clock.instant()
        state.update { s ->
            s.copy(reasons = s.reasons.map { if (it.id == reasonId) it.copy(text = text, updatedAt = now) else it })
        }
    }

    override suspend fun deleteReason(reasonId: String) {
        state.update { s ->
            val gone = s.reasons.firstOrNull { it.id == reasonId } ?: return@update s
            s.copy(reasons = s.reasons - gone, deletedReasons = s.deletedReasons + gone)
        }
    }

    override suspend fun restoreReason(reasonId: String) {
        val now = clock.instant()
        state.update { s ->
            val back = s.deletedReasons.firstOrNull { it.id == reasonId } ?: return@update s
            s.copy(
                reasons = s.reasons + back.copy(updatedAt = now),
                deletedReasons = s.deletedReasons - back,
            )
        }
    }
}
