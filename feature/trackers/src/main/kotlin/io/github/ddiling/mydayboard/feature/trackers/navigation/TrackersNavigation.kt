package io.github.ddiling.mydayboard.feature.trackers.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.list.TrackersListRoute
import kotlinx.serialization.Serializable

/** Route of the tracker list. It has no arguments, hence an object. */
@Serializable
data object TrackersListDestination : Destination

/** Route of one tracker's detail screen. */
@Serializable
data class TrackerDetailDestination(
    val trackerId: String,
) : Destination

/** Route of the tracker editor: [trackerId] null creates a tracker, otherwise edits that one. */
@Serializable
data class TrackerEditorDestination(
    val trackerId: String? = null,
) : Destination

/**
 * Adds the tracker screens to a graph. The app calls this instead of knowing the screens' internals.
 * The detail and editor screens are placeholders until their own changes arrive.
 */
fun NavGraphBuilder.trackersGraph(navController: NavController) {
    composable<TrackersListDestination> {
        TrackersListRoute(
            onTrackerClick = { id -> navController.navigate(TrackerDetailDestination(id)) },
            onAddClick = { navController.navigate(TrackerEditorDestination()) },
        )
    }
    composable<TrackerDetailDestination> { Placeholder(stringResource(R.string.trackers_detail_placeholder)) }
    composable<TrackerEditorDestination> { Placeholder(stringResource(R.string.trackers_editor_placeholder)) }
}

@Composable
private fun Placeholder(text: String) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.Center) { Text(text) }
}
