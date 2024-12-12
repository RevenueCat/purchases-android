package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.ComponentViewState
import com.revenuecat.purchases.ui.revenuecatui.components.ScreenCondition
import com.revenuecat.purchases.ui.revenuecatui.components.state.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.MockResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.util.Locale

@RunWith(AndroidJUnit4::class)
class StackComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

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
            localizationDictionary = emptyMap(),
            locale = Locale.US,
            variables = VariableDataProvider(MockResourceProvider())
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should change background color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(100u), Fixed(100u)),
            backgroundColor = ColorScheme(
                light = ColorInfo.Hex(expectedLightColor.toArgb()),
                dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
            ),
        )

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as StackComponentStyle
            },
            act = { StackComponentView(style = it, modifier = Modifier.testTag("stack")) },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(startX = 0, startY = 0, width = 4, height = 4, color = expectedLightColor)

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(startX = 0, startY = 0, width = 4, height = 4, color = expectedDarkColor)
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should change border color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val sizeDp = 100
        val borderWidthDp = 10.0
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val expectedBackgroundColor = Color.White
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(sizeDp.toUInt()), Fixed(sizeDp.toUInt())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
                ),
                width = borderWidthDp
            ),
        )
        var borderWidthPx: Int? = null
        var sizePx: Int? = null

        themeChangingTest(
            arrange = {
                // Use the Composable context to calculate px equivalents of our dp values.
                borderWidthPx = with(LocalDensity.current) { borderWidthDp.dp.roundToPx() }
                sizePx = with(LocalDensity.current) { sizeDp.dp.roundToPx() }
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component).getOrThrow() as StackComponentStyle
            },
            act = { StackComponentView(style = it, modifier = Modifier.testTag("stack")) },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertSquareBorderColor(
                        sizePx = sizePx!!,
                        borderWidthPx = borderWidthPx!!,
                        expectedBorderColor = expectedLightColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertSquareBorderColor(
                        sizePx = sizePx!!,
                        borderWidthPx = borderWidthPx!!,
                        expectedBorderColor = expectedDarkColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should change shadow color based on theme`(): Unit = with(composeTestRule) {
        // TODO
    }

    /**
     * Asserts the border color of a square element.
     *
     * @param sizePx The size (height & width) of the square element, in px.
     */
    private fun SemanticsNodeInteraction.assertSquareBorderColor(
        sizePx: Int,
        borderWidthPx: Int,
        expectedBorderColor: Color,
        expectedBackgroundColor: Color,
    ): SemanticsNodeInteraction =
        // Top edge
        assertPixelColorEquals(
            startX = 0,
            startY = 0,
            width = sizePx,
            height = borderWidthPx,
            color = expectedBorderColor
        )
            // Left edge
            .assertPixelColorEquals(
                startX = 0,
                startY = 0,
                width = borderWidthPx,
                height = sizePx,
                color = expectedBorderColor
            )
            // Right edge
            .assertPixelColorEquals(
                startX = sizePx - borderWidthPx,
                startY = 0,
                width = borderWidthPx,
                height = sizePx,
                color = expectedBorderColor
            )
            // Bottom edge
            .assertPixelColorEquals(
                startX = 0,
                startY = sizePx - borderWidthPx,
                width = sizePx,
                height = borderWidthPx,
                color = expectedBorderColor
            )
            // Inner area
            .assertPixelColorEquals(
                startX = borderWidthPx,
                startY = borderWidthPx,
                width = sizePx - borderWidthPx - borderWidthPx,
                height = sizePx - borderWidthPx - borderWidthPx,
                color = expectedBackgroundColor
            )
}
