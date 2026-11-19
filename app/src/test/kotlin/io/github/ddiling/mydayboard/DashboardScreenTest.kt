package io.github.ddiling.mydayboard

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.ddiling.mydayboard.core.dashboard.DashboardCard
import io.github.ddiling.mydayboard.core.dashboard.DashboardHost
import io.github.ddiling.mydayboard.feature.counter.navigation.CounterDestination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DashboardScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val host = FakeDashboardHost()
    private lateinit var scope: CoroutineScope

    @Test
    fun `cards appear in the given order`() {
        composeRule.setContent { DashboardScreen(cards = listOf(card("first"), card("second")), host = host) }

        val firstTop =
            composeRule
                .onNodeWithTag("first")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot.top
        val secondTop =
            composeRule
                .onNodeWithTag("second")
                .assertIsDisplayed()
                .fetchSemanticsNode()
                .boundsInRoot.top
        assertTrue(firstTop < secondTop)
    }

    @Test
    fun `the empty state shows when there are no cards`() {
        composeRule.setContent { DashboardScreen(cards = emptyList(), host = host) }

        composeRule.onNodeWithText("Nothing here yet").assertIsDisplayed()
    }

    @Test
    fun `the empty state is hidden when there are cards`() {
        composeRule.setContent { DashboardScreen(cards = listOf(card("first")), host = host) }

        composeRule.onNodeWithText("Nothing here yet").assertDoesNotExist()
    }

    @Test
    fun `a snackbar shown through the host is displayed`() {
        composeRule.setContent {
            scope = rememberCoroutineScope()
            DashboardScreen(cards = emptyList(), host = host)
        }

        composeRule.runOnUiThread { scope.launch { host.snackbarHostState.showSnackbar("Saved") } }

        composeRule.onNodeWithText("Saved").assertIsDisplayed()
    }

    @Test
    fun `the top bar action navigates to the counter`() {
        composeRule.setContent { DashboardScreen(cards = emptyList(), host = host) }

        composeRule.onNodeWithTag(OPEN_COUNTER_TAG).performClick()

        assertEquals(listOf<Any>(CounterDestination), host.navigated)
    }

    private fun card(tag: String) =
        object : DashboardCard {
            override val key = tag
            override val order = 0

            @Composable
            override fun Content(
                host: DashboardHost,
                modifier: Modifier,
            ) = Text(text = tag, modifier = modifier.testTag(tag))
        }
}
