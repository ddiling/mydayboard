package io.github.ddiling.mydayboard.core.domain.id

import java.util.UUID

/**
 * The production [IdGenerator]: a random (version 4) UUID. Unlike a database auto-increment it is unique
 * without asking anyone, so ids stay valid if cloud sync later merges records from two devices.
 */
class UuidIdGenerator : IdGenerator {
    override fun newId(): String = UUID.randomUUID().toString()
}
