package com.revenuecat.purchases.ui.revenuecatui.components.text

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertTextColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

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
            localizationDictionary = localizationDictionary,
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
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as TextComponentStyle
            },
            act = { TextComponentView(style = it, state = state) },
            assert = { theme ->
                theme.setLight()
                onNodeWithText(localizationDictionary.values.first().value)
                    .assertIsDisplayed()
                    .assertTextColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithText(localizationDictionary.values.first().value)
                    .assertIsDisplayed()
                    .assertTextColorEquals(expectedDarkColor)
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should change background color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = TextComponent(
            text = localizationDictionary.keys.first(),
            color = ColorScheme(
                // We're setting the text color to transparent, because our way of checking the background is far from
                // optimal. It just checks a few pixels.
                light = ColorInfo.Hex(Color.Transparent.toArgb()),
            ),
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as TextComponentStyle
            },
            act = { TextComponentView(style = it, state = state) },
            assert = { theme ->
                theme.setLight()
                onNodeWithText(localizationDictionary.values.first().value)
                    .assertIsDisplayed()
                    .assertBackgroundColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithText(localizationDictionary.values.first().value)
                    .assertIsDisplayed()
                    .assertBackgroundColorEquals(expectedDarkColor)
            }
        )
    }

    /**
     * There's some interplay between a Material3 theme and our Markdown component. If both of these are present in the
     * Compose tree, the font size in the Markdown component did not have any effect. This is fixed in #1981.
     * Unfortunately this bug does not show up in Compose Previews. Hence this test to protect against regressions.
     */
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [34])
    @Test
    fun `Should properly set the font size in a Material3 theme`(): Unit = with(composeTestRule) {
        // Arrange
        val textId = localizationDictionary.keys.first()
        val color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))
        val size = Size(Fit, Fit)
        val largeTextComponent = TextComponent(text = textId, color = color, fontSize = FontSize.HEADING_L, size = size)
        val smallTextComponent = TextComponent(text = textId, color = color, fontSize = FontSize.BODY_S, size = size)
        val state = FakePaywallState(largeTextComponent, smallTextComponent)
        setContent {
            val largeTextStyle = styleFactory.create(largeTextComponent).getOrThrow() as TextComponentStyle
            val smallTextStyle = styleFactory.create(smallTextComponent).getOrThrow() as TextComponentStyle

            // Act
            MaterialTheme {
                Column(modifier = Modifier.fillMaxSize()) {
                    TextComponentView(style = largeTextStyle, state = state, modifier = Modifier.testTag("large"))
                    TextComponentView(style = smallTextStyle, state = state, modifier = Modifier.testTag("small"))
                }
            }
        }

        // Assert
        val largeSize = onNodeWithTag("large")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .size

        val smallSize = onNodeWithTag("small")
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .size

        assertThat(largeSize.height).isGreaterThan(smallSize.height)
        assertThat(largeSize.width).isGreaterThan(smallSize.width)
    }

    /**
     * This is a very naive way of checking the background color: by just looking at the 16 top-left pixels. It works
     * for the particular test where it is used, because the color is solid and the text is transparent, but it
     * shouldn't be used more generally.
     *
     * See the documentation for [assertPixelColorEquals] for required annotations.
     */
    private fun SemanticsNodeInteraction.assertBackgroundColorEquals(color: Color): SemanticsNodeInteraction =
        assertPixelColorEquals(startX = 0, startY = 0, width = 4, height = 4, color = color)


}
