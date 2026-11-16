package io.github.ddiling.mydayboard.core.domain

/** What the user has done so far. The UI decides how to phrase it (plurals, i18n). */
sealed interface TapSummary {
    data object None : TapSummary

    data class Some(
        val count: Int,
    ) : TapSummary
}

class SummarizeTapsUseCase {
    operator fun invoke(count: Int): TapSummary {
        require(count >= 0) { "count must not be negative, was $count" }
        return if (count == 0) TapSummary.None else TapSummary.Some(count)
    }
}
