package io.github.ddiling.mydayboard

import androidx.compose.material3.SnackbarHostState
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost

/** Records what cards ask of the dashboard. */
class FakeDashboardHost : DashboardHost {
    override val snackbarHostState = SnackbarHostState()
    val navigated = mutableListOf<Any>()

    override fun navigate(destination: Any) {
        navigated += destination
    }
}
