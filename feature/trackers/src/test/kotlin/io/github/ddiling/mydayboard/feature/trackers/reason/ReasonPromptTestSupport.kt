package io.github.ddiling.mydayboard.feature.trackers.reason

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository
import io.github.ddiling.mydayboard.core.testing.FakeTrackerReasonRepository
import io.github.ddiling.mydayboard.core.testing.SequentialIdGenerator
import java.time.Clock
import kotlin.random.Random

/** A prompter over its own empty reasons: for tests whose trackers keep the switch off and never open the dialog. */
internal fun reasonPrompter(
    trackers: TrackerRepository,
    clock: Clock,
): ReasonPrompter = ReasonPrompter(trackers, FakeTrackerReasonRepository(clock, SequentialIdGenerator()), Random(0))
