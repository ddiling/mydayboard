package io.github.ddiling.mydayboard.core.domain

import javax.inject.Inject

/** What the user has done so far. The UI decides how to phrase it (plurals, i18n). */
sealed interface TapSummary {
    data object None : TapSummary

    data class Some(
        val count: Int,
    ) : TapSummary
}

/**
 * Turns a tap count into a [TapSummary].
 *
 * `@Inject constructor()` lets Hilt create it for any ViewModel that asks for it, with no module to write.
 * It has no state, so every injection gets a new, cheap instance.
 */
class SummarizeTapsUseCase
    @Inject
    constructor() {
        operator fun invoke(count: Int): TapSummary {
            require(count >= 0) { "count must not be negative, was $count" }
            return if (count == 0) TapSummary.None else TapSummary.Some(count)
        }
    }
