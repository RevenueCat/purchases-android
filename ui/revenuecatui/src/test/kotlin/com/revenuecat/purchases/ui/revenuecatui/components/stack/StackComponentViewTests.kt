package com.revenuecat.purchases.ui.revenuecatui.components.stack

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Border
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Shadow
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertApproximatePixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertNoPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertRectangularBorderColor
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class StackComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val styleFactory = StyleFactory(
        localizations = nonEmptyMapOf(
            LocaleId("en_US") to nonEmptyMapOf(
                LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
            )
        )
    )
    private val actionHandler: (PaywallAction) -> Unit = {}

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
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component, actionHandler).getOrThrow() as StackComponentStyle
            },
            act = { StackComponentView(style = it, state = state, modifier = Modifier.testTag("stack")) },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedLightColor)

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedDarkColor)
            }
        )
    }

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
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component, actionHandler).getOrThrow() as StackComponentStyle
            },
            act = { StackComponentView(style = it, state = state, modifier = Modifier.testTag("stack")) },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertRectangularBorderColor(
                        borderWidth = borderWidthDp.dp,
                        expectedBorderColor = expectedLightColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )

                theme.setDark()
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    .assertRectangularBorderColor(
                        borderWidth = borderWidthDp.dp,
                        expectedBorderColor = expectedDarkColor,
                        expectedBackgroundColor = expectedBackgroundColor,
                    )
            }
        )
    }

    @Test
    fun `Should change shadow color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSizeDp = 200
        val stackSizeDp = 100
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow
        val expectedBackgroundColor = Color.White
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSizeDp.toUInt()), Fixed(stackSizeDp.toUInt())),
            shadow = Shadow(
                color = ColorScheme(
                    light = ColorInfo.Hex(expectedLightColor.toArgb()),
                    dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
                ),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedBackgroundColor.toArgb())),
        )
        val state = FakePaywallState(component)

        themeChangingTest(
            arrange = {
                // We don't want to recreate the entire tree every time the theme, or any other state, changes.
                styleFactory.create(component, actionHandler).getOrThrow() as StackComponentStyle
            },
            act = {
                // An outer box, because a shadow draws outside the Composable's bounds.
                Box(
                    modifier = Modifier
                        .testTag(tag = "parent")
                        .requiredSize(parentSizeDp.dp)
                        .background(expectedBackgroundColor),
                    contentAlignment = Alignment.Center,
                ) {
                    StackComponentView(style = it, state = state, modifier = Modifier.testTag("stack"))
                }
            },
            assert = { theme ->
                onNodeWithTag("stack")
                    .assertIsDisplayed()
                    // No inner shadow, so the entire stack should be the same color.
                    .assertPixelColorEquals(expectedBackgroundColor)

                theme.setLight()
                onNodeWithTag("parent")
                    .assertIsDisplayed()
                    // When the shadow is drawn, at least some pixels are the exact color we're looking for.
                    .assertApproximatePixelColorPercentage(expectedLightColor, threshold = 0.1f) { it > 0f }
                    .assertNoPixelColorEquals(expectedDarkColor)

                theme.setDark()
                onNodeWithTag("parent")
                    .assertIsDisplayed()
                    // When the shadow is drawn, at least some pixels are the exact color we're looking for.
                    .assertApproximatePixelColorPercentage(expectedDarkColor, threshold = 0.1f) { it > 0f }
                    .assertNoPixelColorEquals(expectedLightColor)
            }
        )
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the selected overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSizeDp = 200u
        val stackSizeDp = 100u
        val expectedUnselectedBorderColor = Color.Black
        val expectedSelectedBorderColor = Color.Cyan
        val expectedUnselectedBorderWidth = 2.0
        val expectedSelectedBorderWidth = 4.0
        val expectedUnselectedShadowColor = Color.Yellow
        val expectedSelectedShadowColor = Color.Red
        val expectedSelectedBackgroundColor = Color.Blue
        val expectedUnselectedBackgroundColor = Color.Green
        val parentBackgroundColor = Color.Magenta
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSizeDp), Fixed(stackSizeDp)),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedUnselectedBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedBorderColor.toArgb())),
                width = expectedUnselectedBorderWidth
            ),
            shadow = Shadow(
                color = ColorScheme(light = ColorInfo.Hex(expectedUnselectedShadowColor.toArgb())),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            overrides = ComponentOverrides(
                states = ComponentStates(
                    selected = PartialStackComponent(
                        backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedSelectedBackgroundColor.toArgb())),
                        border = Border(
                            color = ColorScheme(light = ColorInfo.Hex(expectedSelectedBorderColor.toArgb())),
                            width = expectedSelectedBorderWidth
                        ),
                        shadow = Shadow(
                            color = ColorScheme(light = ColorInfo.Hex(expectedSelectedShadowColor.toArgb())),
                            radius = 5.0,
                            x = 10.0,
                            y = 10.0,
                        ),
                    ),
                ),
            )
        )
        val state = FakePaywallState(component)
        val style = styleFactory.create(component).getOrThrow() as StackComponentStyle

        // Act
        setContent {
            var selected by remember { mutableStateOf(false) }
            // An outer box, because a shadow draws outside the Composable's bounds.
            Box(
                modifier = Modifier
                    .testTag(tag = "parent")
                    .requiredSize(parentSizeDp.toInt().dp)
                    .background(parentBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                StackComponentView(
                    style = style,
                    state = state,
                    selected = selected,
                    modifier = Modifier.testTag("stack")
                )
            }
            Switch(checked = selected, onCheckedChange = { selected = it }, modifier = Modifier.testTag("switch"))
        }

        // Assert
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedUnselectedBorderWidth.dp,
                expectedBorderColor = expectedUnselectedBorderColor,
                expectedBackgroundColor = expectedUnselectedBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedUnselectedShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedSelectedShadowColor)

        // Change `selected` to true
        onNodeWithTag("switch")
            .performClick()

        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedSelectedBorderWidth.dp,
                expectedBorderColor = expectedSelectedBorderColor,
                expectedBackgroundColor = expectedSelectedBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedSelectedShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedUnselectedShadowColor)
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @Test
    fun `Should use the intro offer overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val parentSizeDp = 200
        val stackSizeDp = 100
        val expectedIneligibleBorderColor = Color.Black
        val expectedEligibleBorderColor = Color.Cyan
        val expectedIneligibleBorderWidth = 2.0
        val expectedEligibleBorderWidth = 4.0
        val expectedIneligibleShadowColor = Color.Yellow
        val expectedEligibleShadowColor = Color.Red
        val expectedEligibleBackgroundColor = Color.Blue
        val expectedIneligibleBackgroundColor = Color.Green
        val parentBackgroundColor = Color.Magenta
        val component = StackComponent(
            components = emptyList(),
            size = Size(Fixed(stackSizeDp.toUInt()), Fixed(stackSizeDp.toUInt())),
            backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBackgroundColor.toArgb())),
            border = Border(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleBorderColor.toArgb())),
                width = expectedIneligibleBorderWidth
            ),
            shadow = Shadow(
                color = ColorScheme(light = ColorInfo.Hex(expectedIneligibleShadowColor.toArgb())),
                radius = 5.0,
                x = 10.0,
                y = 10.0,
            ),
            overrides = ComponentOverrides(
                introOffer = PartialStackComponent(
                    backgroundColor = ColorScheme(light = ColorInfo.Hex(expectedEligibleBackgroundColor.toArgb())),
                    border = Border(
                        color = ColorScheme(light = ColorInfo.Hex(expectedEligibleBorderColor.toArgb())),
                        width = expectedEligibleBorderWidth
                    ),
                    shadow = Shadow(
                        color = ColorScheme(light = ColorInfo.Hex(expectedEligibleShadowColor.toArgb())),
                        radius = 5.0,
                        x = 10.0,
                        y = 10.0,
                    ),
                ),
            )
        )
        val state = FakePaywallState(component)
        val style = styleFactory.create(component).getOrThrow() as StackComponentStyle

        // Act
        setContent {
            // An outer box, because a shadow draws outside the Composable's bounds.
            Box(
                modifier = Modifier
                    .testTag(tag = "parent")
                    .requiredSize(parentSizeDp.dp)
                    .background(parentBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                StackComponentView(style = style, state = state, modifier = Modifier.testTag("stack"))
            }
        }

        // Assert
        state.update(isEligibleForIntroOffer = false)
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedIneligibleBorderWidth.dp,
                expectedBorderColor = expectedIneligibleBorderColor,
                expectedBackgroundColor = expectedIneligibleBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedIneligibleShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedEligibleShadowColor)

        state.update(isEligibleForIntroOffer = true)
        onNodeWithTag("stack")
            .assertIsDisplayed()
            .assertRectangularBorderColor(
                borderWidth = expectedEligibleBorderWidth.dp,
                expectedBorderColor = expectedEligibleBorderColor,
                expectedBackgroundColor = expectedEligibleBackgroundColor,
            )
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorPercentage(expectedEligibleShadowColor) { it > 0f }
            .assertNoPixelColorEquals(expectedIneligibleShadowColor)
    }
}
