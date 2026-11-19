package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import io.github.ddiling.mydayboard.core.domain.tracker.TrackerRepository

private fun fake(
    clock: MutableClock,
    ids: IdGenerator,
): TrackerRepository = FakeTrackerRepository(clock, ids)

class FakeTrackerContractTest : TrackerContractTest() {
    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fake(clock, ids)
}

class FakeTrackerEventContractTest : TrackerEventContractTest() {
    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fake(clock, ids)
}

class FakeTrackerNoOpContractTest : TrackerNoOpContractTest() {
    override fun createRepository(
        clock: MutableClock,
        ids: IdGenerator,
    ): TrackerRepository = fake(clock, ids)
}
