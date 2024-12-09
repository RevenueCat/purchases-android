package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

/**
 * Test Composable behavior across theme changes.
 *
 * @param arrange Return any setup data that needs to be built outside of the themable content.
 * @param act Build the Composable content that is expected to react to theme changes here. Receives the return value
 * from [arrange].
 * @param assert Assert the content built in [act]. The provided [ThemeController] can be used to control the active
 * theme.
 */
internal fun <T> ComposeContentTestRule.themeChangingTest(
    arrange: @Composable () -> T,
    act: @Composable (T) -> Unit,
    assert: ComposeTestRule.(ThemeController) -> Unit,
) {
    setContent {
        val baseConfiguration: Configuration = LocalConfiguration.current
        val lightModeConfiguration = Configuration(baseConfiguration)
            .apply { uiMode = setUiModeToLightTheme(baseConfiguration.uiMode) }
        val darkModeConfiguration = Configuration(baseConfiguration)
            .apply { uiMode = setUiModeToDarkTheme(baseConfiguration.uiMode) }

        var darkTheme by mutableStateOf(false)
        val configuration by remember {
            derivedStateOf { if (darkTheme) darkModeConfiguration else lightModeConfiguration }
        }

        val arrangeResult = arrange()

        CompositionLocalProvider(LocalConfiguration provides configuration) {
            // A TextComponentView and a button to change the theme.
            Column {
                act(arrangeResult)
                Button(onClick = { darkTheme = !darkTheme }) { Text("Toggle") }
            }
        }
    }

    assert(ThemeController(this))
}

internal class ThemeController(private val composeTestRule: ComposeTestRule) {
    fun toggleTheme() {
        composeTestRule
            .onNodeWithText("Toggle")
            .performClick()
    }
}

private fun setUiModeToDarkTheme(uiMode: Int): Int =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES

private fun setUiModeToLightTheme(uiMode: Int): Int =
    (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
