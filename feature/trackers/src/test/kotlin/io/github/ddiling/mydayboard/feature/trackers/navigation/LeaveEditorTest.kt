package io.github.ddiling.mydayboard.feature.trackers.navigation

import android.content.Context
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import io.github.ddiling.mydayboard.feature.trackers.editor.EditorResult
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LeaveEditorTest {
    private fun givenNavController(): NavHostController {
        val controller = NavHostController(ApplicationProvider.getApplicationContext<Context>())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.graph =
            controller.createGraph(startDestination = TrackersListDestination) {
                composable<TrackersListDestination> {}
                composable<TrackerDetailDestination> {}
                composable<TrackerEditorDestination> {}
            }
        return controller
    }

    private fun NavHostController.routes(): List<String> =
        currentBackStack.value
            .filter { it.destination.route != null }
            .map {
                it.destination.route
                    .orEmpty()
                    .substringBefore('/')
                    .substringBefore('?')
                    .substringAfterLast('.')
            }

    @Test
    fun `saving a new tracker replaces the editor with its detail screen`() {
        val controller = givenNavController()
        controller.navigate(TrackerEditorDestination())

        controller.leaveEditor(EditorResult.Saved("tracker-1"), isEditing = false)

        assertEquals(listOf("TrackersListDestination", "TrackerDetailDestination"), controller.routes())
        assertEquals("tracker-1", controller.currentBackStackEntry?.toRoute<TrackerDetailDestination>()?.trackerId)
    }

    @Test
    fun `saving an edited tracker goes back to its detail screen`() {
        val controller = givenNavController()
        controller.navigate(TrackerDetailDestination("tracker-1"))
        controller.navigate(TrackerEditorDestination("tracker-1"))

        controller.leaveEditor(EditorResult.Saved("tracker-1"), isEditing = true)

        assertEquals(listOf("TrackersListDestination", "TrackerDetailDestination"), controller.routes())
    }

    @Test
    fun `deleting a tracker goes back to the list and drops its detail screen`() {
        val controller = givenNavController()
        controller.navigate(TrackerDetailDestination("tracker-1"))
        controller.navigate(TrackerEditorDestination("tracker-1"))

        controller.leaveEditor(EditorResult.Deleted, isEditing = true)

        assertEquals(listOf("TrackersListDestination"), controller.routes())
    }
}
