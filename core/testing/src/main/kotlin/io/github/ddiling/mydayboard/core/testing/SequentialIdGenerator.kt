package io.github.ddiling.mydayboard.core.testing

import io.github.ddiling.mydayboard.core.domain.id.IdGenerator
import java.util.concurrent.atomic.AtomicInteger

/**
 * An [IdGenerator] with predictable ids (`id-1`, `id-2`, ...), so a test can name the entity it expects
 * instead of copying a random UUID out of the result.
 * The counter is atomic because code under test may create entities from more than one thread.
 */
class SequentialIdGenerator(
    private val prefix: String = "id",
) : IdGenerator {
    private val lastNumber = AtomicInteger(0)

    override fun newId(): String = "$prefix-${lastNumber.incrementAndGet()}"
}
