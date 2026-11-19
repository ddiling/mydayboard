package io.github.ddiling.mydayboard.core.dashboard

/**
 * Cards in display order: by [DashboardCard.order], then [DashboardCard.key].
 * A set has no order, so the key tie-break makes equal orders deterministic.
 * Duplicate keys are a programming error in two features, and would make the order ambiguous, so they fail fast.
 */
fun sortedDashboardCards(cards: Set<DashboardCard>): List<DashboardCard> {
    val duplicateKeys = cards.groupBy { it.key }.filterValues { it.size > 1 }.keys
    require(
        duplicateKeys.isEmpty(),
    ) { "Dashboard cards must have unique keys, but these are used twice: $duplicateKeys" }
    return cards.sortedWith(compareBy({ it.order }, { it.key }))
}
