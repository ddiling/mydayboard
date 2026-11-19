package io.github.ddiling.mydayboard.core.testing

import org.junit.Assert.assertEquals
import org.junit.Test

class SequentialIdGeneratorTest {
    @Test
    fun `ids are numbered from 1 in the order they are created`() {
        val generator = SequentialIdGenerator()

        val ids = List(3) { generator.newId() }

        assertEquals(listOf("id-1", "id-2", "id-3"), ids)
    }

    @Test
    fun `a custom prefix replaces id`() {
        val generator = SequentialIdGenerator(prefix = "tracker")

        val id = generator.newId()

        assertEquals("tracker-1", id)
    }
}
