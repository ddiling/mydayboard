package io.github.ddiling.mydayboard

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `settings screen shows the coming soon text`() {
        composeRule.setContent { SettingsScreen(onBack = {}) }

        composeRule.onNodeWithText("Settings coming soon").assertIsDisplayed()
    }
}
