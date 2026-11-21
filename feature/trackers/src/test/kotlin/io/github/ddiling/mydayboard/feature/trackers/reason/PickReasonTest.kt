package io.github.ddiling.mydayboard.feature.trackers.reason

import io.github.ddiling.mydayboard.core.domain.tracker.TrackerReason
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant
import kotlin.random.Random

class PickReasonTest {
    private val sleepBetter = reason("r1", "I sleep better")
    private val saveMoney = reason("r2", "I save money")
    private val moreEnergy = reason("r3", "I have more energy")

    private fun reason(
        id: String,
        text: String,
    ) = TrackerReason(id, "t1", text, Instant.EPOCH, Instant.EPOCH)

    @Test
    fun `no reasons gives nothing`() {
        assertNull(pickReason(emptyList<TrackerReason>(), currentId = null, Random(1)))
    }

    @Test
    fun `the only reason is returned even when it is the current one`() {
        assertEquals(sleepBetter, pickReason(listOf(sleepBetter), currentId = sleepBetter.id, Random(1)))
    }

    @Test
    fun `with two or more reasons the current one is never picked`() {
        val reasons = listOf(sleepBetter, saveMoney, moreEnergy)

        val picks = (0 until 50).map { pickReason(reasons, currentId = saveMoney.id, Random(it)) }

        assertEquals(emptyList<TrackerReason?>(), picks.filter { it == null || it.id == saveMoney.id })
    }

    @Test
    fun `the same seed picks the same reason`() {
        val reasons = listOf(sleepBetter, saveMoney, moreEnergy)

        assertEquals(pickReason(reasons, null, Random(7)), pickReason(reasons, null, Random(7)))
    }

    @Test
    fun `different seeds can pick different reasons`() {
        val reasons = listOf(sleepBetter, saveMoney, moreEnergy)

        val picked = (0 until 50).map { pickReason(reasons, null, Random(it)) }.toSet()

        assertNotEquals(1, picked.size)
    }
}
