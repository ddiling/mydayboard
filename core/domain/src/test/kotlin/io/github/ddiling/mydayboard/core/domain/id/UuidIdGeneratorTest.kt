package io.github.ddiling.mydayboard.core.domain.id

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class UuidIdGeneratorTest {
    private val generator = UuidIdGenerator()

    @Test
    fun `id is a random UUID in its standard text form`() {
        val id = generator.newId()

        val uuid = UUID.fromString(id)
        assertEquals(id, uuid.toString())
        assertEquals(4, uuid.version())
    }

    @Test
    fun `a thousand ids are all different`() {
        val ids = List(1_000) { generator.newId() }

        assertEquals(1_000, ids.toSet().size)
    }
}
