package com.revenuecat.purchases.ui.revenuecatui.helpers

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTextReplacement
import java.util.Locale

/**
 * Test Composable behavior across locale changes without recreating the Activity. This scenario occurs if the Activity
 * has both `locale` and `layoutDirection` defined in the `configChanges` attribute in the manifest.
 *
 * @param arrange Return any setup data that needs to be built outside of the locale-dependent content.
 * @param act Build the Composable content that is expected to react to locale changes here. Receives the return value
 * from [arrange].
 * @param assert Assert the content built in [act]. The provided [LocaleController] can be used to control the active
 * theme.
 */
internal fun <T> ComposeContentTestRule.localeChangingTest(
    arrange: @Composable () -> T,
    act: @Composable (T) -> Unit,
    assert: ComposeTestRule.(LocaleController) -> Unit,
) {
    setContent {
        val baseConfiguration: Configuration = LocalConfiguration.current
        var languageTag by mutableStateOf(baseConfiguration.locales[0].toLanguageTag())
        val configuration by remember {
            derivedStateOf { Configuration(baseConfiguration).apply { setLocale(Locale.forLanguageTag(languageTag)) } }
        }

        val arrangeResult = arrange()

        CompositionLocalProvider(LocalConfiguration provides configuration) {
            // The content under test, and a TextField to change the locale.
            Column {
                act(arrangeResult)
                TextField(
                    value = languageTag,
                    onValueChange = { languageTag = it },
                    modifier = Modifier.testTag("languageTag")
                )
            }
        }
    }

    assert(LocaleController(this))
}

internal class LocaleController(private val composeTestRule: ComposeTestRule) {
    fun setLocale(languageTag: String) {
        composeTestRule
            .onNodeWithTag("languageTag")
            .performTextReplacement(text = languageTag)
    }

}
