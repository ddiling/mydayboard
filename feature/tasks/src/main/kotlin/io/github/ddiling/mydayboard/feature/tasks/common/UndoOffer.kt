package io.github.ddiling.mydayboard.feature.tasks.common

import io.github.ddiling.mydayboard.core.domain.task.TickOutcome

/**
 * Message after a swipe: the task's [name] and what the swipe did, so the snackbar can say "Send CVs 2/3" or
 * "Breakfast done" and hand [outcome] back to Undo. The name is kept here because the row may be gone by the
 * time the message shows.
 */
internal data class UndoOffer(
    val name: String,
    val outcome: TickOutcome.Added,
) {
    /** True when this swipe finished the task (all counts or all times done). */
    val completes: Boolean get() = outcome.done >= outcome.target
}
