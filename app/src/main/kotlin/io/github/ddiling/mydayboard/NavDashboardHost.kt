package io.github.ddiling.mydayboard

import androidx.compose.material3.SnackbarHostState
import androidx.navigation.NavHostController
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.core.navigation.Destination

/** The real [DashboardHost]: cards navigate through the app's controller without knowing it exists. */
internal class NavDashboardHost(
    private val navController: NavHostController,
) : DashboardHost {
    override val snackbarHostState = SnackbarHostState()

    // Single top: a double tap must not stack the same destination twice.
    override fun navigate(destination: Destination) = navController.navigate(destination) { launchSingleTop = true }
}
