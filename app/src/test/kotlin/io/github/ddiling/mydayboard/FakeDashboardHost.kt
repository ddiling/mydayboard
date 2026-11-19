package io.github.ddiling.mydayboard

import androidx.compose.material3.SnackbarHostState
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.navigation.Destination

/** Records what cards ask of the dashboard. */
class FakeDashboardHost : DashboardHost {
    override val snackbarHostState = SnackbarHostState()
    val navigated = mutableListOf<Destination>()

    override fun navigate(destination: Destination) {
        navigated += destination
    }
}
