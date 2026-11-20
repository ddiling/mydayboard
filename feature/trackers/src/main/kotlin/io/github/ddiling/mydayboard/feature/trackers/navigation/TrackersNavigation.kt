package io.github.ddiling.mydayboard.feature.trackers.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import io.github.ddiling.mydayboard.core.navigation.Destination
import io.github.ddiling.mydayboard.feature.trackers.R
import io.github.ddiling.mydayboard.feature.trackers.editor.EditorResult
import io.github.ddiling.mydayboard.feature.trackers.editor.TrackerEditorRoute
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
 * The detail screen is a placeholder until its own change arrives; it only offers the way into the editor.
 * Leaving the editor: a created tracker replaces the editor with its detail screen (so Back does not return to
 * an empty form), a rename goes back, and a delete goes back to the list because the detail screen under the
 * editor would show a tracker that no longer exists.
 */
fun NavGraphBuilder.trackersGraph(navController: NavController) {
    composable<TrackersListDestination> {
        TrackersListRoute(
            onTrackerClick = { id -> navController.navigate(TrackerDetailDestination(id)) },
            onAddClick = { navController.navigate(TrackerEditorDestination()) },
        )
    }
    composable<TrackerDetailDestination> { entry ->
        val id = entry.toRoute<TrackerDetailDestination>().trackerId
        DetailPlaceholder { navController.navigate(TrackerEditorDestination(id)) }
    }
    composable<TrackerEditorDestination> {
        TrackerEditorRoute(
            onResult = { result, isEditing -> navController.leaveEditor(result, isEditing) },
            onBack = { navController.popBackStack() },
        )
    }
}

private fun NavController.leaveEditor(
    result: EditorResult,
    isEditing: Boolean,
) {
    when {
        result == EditorResult.Deleted -> {
            popBackStack(TrackersListDestination, inclusive = false)
        }

        isEditing -> {
            popBackStack()
        }

        result is EditorResult.Saved -> {
            navigate(TrackerDetailDestination(result.trackerId)) {
                popUpTo<TrackerEditorDestination> { inclusive = true }
            }
        }
    }
}

@Composable
private fun DetailPlaceholder(onEditClick: () -> Unit) {
    Column(
        Modifier.fillMaxSize().safeDrawingPadding(),
        Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        Alignment.CenterHorizontally,
    ) {
        Text(stringResource(R.string.trackers_detail_placeholder))
        Button(onClick = onEditClick) { Text(stringResource(R.string.trackers_edit)) }
    }
}
