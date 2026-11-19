package io.github.ddiling.mydayboard.core.dashboard

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.ddiling.mydayboard.core.navigation.Destination

/**
 * One card on the dashboard. Features contribute cards with Hilt `@IntoSet`; `:app` renders them,
 * so `:app` never needs to know what a feature's card shows.
 */
interface DashboardCard {
    /** Stable and unique across all cards (for example "trackers"); it is the tie-breaker when sorting. */
    val key: String

    /** Ascending position on the dashboard. */
    val order: Int

    @Composable
    fun Content(
        host: DashboardHost,
        modifier: Modifier,
    )
}

/** What a card may ask of the dashboard, without knowing `:app`. */
interface DashboardHost {
    val snackbarHostState: SnackbarHostState

    /** [destination] is a `@Serializable` [Destination] owned by the card's feature. */
    fun navigate(destination: Destination)
}
