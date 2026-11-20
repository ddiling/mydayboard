package io.github.ddiling.mydayboard.feature.trackers.detail

import androidx.activity.ComponentActivity
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import io.github.ddiling.mydayboard.feature.trackers.R
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UndoSnackbarTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val host = SnackbarHostState()
    private var dismissals = 0
    private var pendingUndo by mutableStateOf<Undo?>(Undo("event-1", UndoKind.ADDED))
    private var showSnackbar by mutableStateOf(true)

    private fun givenUndoSnackbar() {
        composeRule.setContent {
            if (showSnackbar) {
                UndoSnackbar(
                    undo = pendingUndo,
                    host = host,
                    onUndo = {},
                    onUndoDismissed = { dismissals++ },
                )
            }
        }
    }

    @Test
    fun `a newer pending undo replaces the shown snackbar without dismissing the old one`() {
        givenUndoSnackbar()
        composeRule.waitForIdle()

        pendingUndo = Undo("event-2", UndoKind.RESET)
        composeRule.waitForIdle()

        assertEquals(
            composeRule.activity.getString(R.string.trackers_detail_reset_done),
            host.currentSnackbarData?.visuals?.message,
        )
        assertEquals(0, dismissals)
    }

    @Test
    fun `leaving composition with a pending undo reports it dismissed once`() {
        givenUndoSnackbar()
        composeRule.waitForIdle()

        showSnackbar = false
        composeRule.waitForIdle()

        assertEquals(1, dismissals)
    }

    @Test
    fun `leaving composition with no pending undo reports nothing`() {
        pendingUndo = null
        givenUndoSnackbar()
        composeRule.waitForIdle()

        showSnackbar = false
        composeRule.waitForIdle()

        assertEquals(0, dismissals)
    }

    @Test
    fun `recreating the activity does not report the pending undo as dismissed`() {
        givenUndoSnackbar()

        composeRule.activityRule.scenario.recreate()

        assertEquals(0, dismissals)
    }
}
