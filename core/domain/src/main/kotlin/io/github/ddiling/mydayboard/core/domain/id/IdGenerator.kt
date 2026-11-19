package io.github.ddiling.mydayboard.core.domain.id

/**
 * Creates the ids of new entities (trackers, entries, ...). An interface instead of calling `UUID.randomUUID()`
 * where an entity is created: production code gets random UUIDs ([UuidIdGenerator]), while tests pass
 * a generator with predictable ids they can assert on.
 */
fun interface IdGenerator {
    /** Returns an id that no earlier call returned. */
    fun newId(): String
}
