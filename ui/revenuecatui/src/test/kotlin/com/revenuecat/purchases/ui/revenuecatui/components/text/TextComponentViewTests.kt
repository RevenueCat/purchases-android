package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.content.res.Configuration
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class TextComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localizationDictionary = mapOf(
        LocalizationKey("text1") to LocalizationData.Text("this is text 1"),
    )
    private lateinit var styleFactory: StyleFactory

    @Before
    fun setup() {
        styleFactory = StyleFactory(
            windowSize = ScreenCondition.COMPACT,
            isEligibleForIntroOffer = true,
            componentState = ComponentViewState.DEFAULT,
            packageContext = PackageContext(
                initialSelectedPackage = null,
                initialVariableContext = PackageContext.VariableContext(
                    packages = emptyList(),
                    showZeroDecimalPlacePrices = false
                )
            ),
            localizationDictionary = localizationDictionary,
            locale = Locale.US,
            variables = VariableDataProvider(MockResourceProvider())
        )
    }

    @Test
    fun `Should change text color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = TextComponent(
            text = localizationDictionary.keys.first(),
            color = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )

        setContent {
            // We don't want to recreate the entire tree every time the theme, or any other state, changes.
            val style = styleFactory.create(component).getOrThrow() as TextComponentStyle

            val baseConfiguration: Configuration = LocalConfiguration.current
            val lightModeConfiguration = Configuration(baseConfiguration)
                .apply { uiMode = setUiModeToLightTheme(baseConfiguration.uiMode) }
            val darkModeConfiguration = Configuration(baseConfiguration)
                .apply { uiMode = setUiModeToDarkTheme(baseConfiguration.uiMode) }

            var darkTheme by mutableStateOf(false)
            val configuration by remember {
                derivedStateOf { if (darkTheme) darkModeConfiguration else lightModeConfiguration }
            }

            // Act
            CompositionLocalProvider(LocalConfiguration provides configuration) {
                // A TextComponentView and a button to change the theme.
                Column {
                    TextComponentView(style)
                    Button(onClick = { darkTheme = !darkTheme }) { Text("Toggle") }
                }
            }
        }

        // Assert
        onNodeWithText(localizationDictionary.values.first().value)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedLightColor)

        // Change the theme.
        onNodeWithText("Toggle")
            .performClick()

        onNodeWithText(localizationDictionary.values.first().value)
            .assertIsDisplayed()
            .assertTextColorEquals(expectedDarkColor)
    }

    private fun setUiModeToDarkTheme(uiMode: Int): Int =
        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_YES

    private fun setUiModeToLightTheme(uiMode: Int): Int =
        (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO

    private fun SemanticsNodeInteraction.assertTextColorEquals(color: Color) =
        assert(
            SemanticsMatcher(
                "${SemanticsProperties.Text.name} has color '$color'"
            ) { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)

                if (results.isEmpty()) false
                else results.first().layoutInput.style.color == color
            }
        )
}
