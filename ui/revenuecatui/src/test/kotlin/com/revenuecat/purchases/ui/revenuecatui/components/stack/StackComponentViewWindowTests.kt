package com.revenuecat.purchases.ui.revenuecatui.components.stack

import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentConditions
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
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
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertRectangularBorderColor
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowChangingTest
import org.junit.Rule
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import org.junit.runner.RunWith
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

@RunWith(Enclosed::class)
internal class StackComponentViewWindowTests {

    private companion object {
        // The following values are taken from WindowWidthSizeClass.kt (minus or plus 1).
        private const val MAX_WIDTH_COMPACT = 599
        private const val MAX_WIDTH_MEDIUM = 839
        private const val MIN_WIDTH_EXPANDED = 841

        private const val SIZE_PARENT = 200u
        private const val SIZE_STACK = 100u
        private val parentBackgroundColor = Color.White
        private val expectedCompactBorderColor = Color.Black
        private val expectedMediumBorderColor = Color.DarkGray
        private val expectedExpandedBorderColor = Color.LightGray
        private val expectedCompactBorderWidth = 2.0.dp
        private val expectedMediumBorderWidth = 6.0.dp
        private val expectedExpandedBorderWidth = 8.0.dp
        private val expectedCompactShadowColor = Color.Yellow
        private val expectedMediumShadowColor = Color.Red
        private val expectedExpandedShadowColor = Color.Green
        private val expectedCompactBackgroundColor = Color.Magenta
        private val expectedMediumBackgroundColor = Color.Cyan
        private val expectedExpandedBackgroundColor = Color.Gray

        val component = StackComponent(
            components = emptyList(),
            size = Size(width = Fixed(SIZE_STACK), height = Fixed(SIZE_STACK)),
            overrides = ComponentOverrides(
                conditions = ComponentConditions(
                    compact = PartialStackComponent(
                        border = Border(
                            color = ColorScheme(ColorInfo.Hex(expectedCompactBorderColor.toArgb())),
                            width = expectedCompactBorderWidth.value.toDouble()
                        ),
                        shadow = Shadow(
                            color = ColorScheme(ColorInfo.Hex(expectedCompactShadowColor.toArgb())),
                            radius = 5.0,
                            x = 10.0,
                            y = 10.0
                        ),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedCompactBackgroundColor.toArgb()))
                    ),
                    medium = PartialStackComponent(
                        border = Border(
                            color = ColorScheme(ColorInfo.Hex(expectedMediumBorderColor.toArgb())),
                            width = expectedMediumBorderWidth.value.toDouble()
                        ),
                        shadow = Shadow(
                            color = ColorScheme(ColorInfo.Hex(expectedMediumShadowColor.toArgb())),
                            radius = 5.0,
                            x = 10.0,
                            y = 10.0
                        ),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedMediumBackgroundColor.toArgb()))
                    ),
                    expanded = PartialStackComponent(
                        border = Border(
                            color = ColorScheme(ColorInfo.Hex(expectedExpandedBorderColor.toArgb())),
                            width = expectedExpandedBorderWidth.value.toDouble()
                        ),
                        shadow = Shadow(
                            color = ColorScheme(ColorInfo.Hex(expectedExpandedShadowColor.toArgb())),
                            radius = 5.0,
                            x = 10.0,
                            y = 10.0
                        ),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedExpandedBackgroundColor.toArgb()))
                    )
                )
            )
        )
        val state = FakePaywallState(component)
        val styleFactory = StyleFactory(
            localizations = nonEmptyMapOf(
                LocaleId("en_US") to nonEmptyMapOf(
                    LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
                )
            ),
            offering = Offering(
                identifier = "identifier",
                serverDescription = "description",
                metadata = emptyMap(),
                availablePackages = emptyList(),
            )
        )
        val style = styleFactory.create(component, { }).getOrThrow() as StackComponentStyle
        val content = @Composable {
            // An outer box, because a shadow draws outside the Composable's bounds.
            Box(
                modifier = Modifier
                    .testTag(tag = "parent")
                    .requiredSize(SIZE_PARENT.toInt().dp)
                    .background(parentBackgroundColor),
                contentAlignment = Alignment.Center,
            ) {
                StackComponentView(style, state, { }, modifier = Modifier.testTag("stack"))
            }
        }

        fun SemanticsNodeInteractionsProvider.assertCompactOverrideIsDisplayed() {
            onNodeWithTag("stack")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedCompactBorderWidth,
                    expectedBorderColor = expectedCompactBorderColor,
                    expectedBackgroundColor = expectedCompactBackgroundColor,
                )
            // Assert the shadow on the parent, because it is drawn outside of the stack's bounding box.
            onNodeWithTag("parent")
                .assertIsDisplayed()
                // When the shadow is drawn, at least some pixels are the approximate color we're looking for.
                .assertApproximatePixelColorPercentage(expectedCompactShadowColor, threshold = 0.1f) { it > 0f }
                .assertNoPixelColorEquals(expectedMediumShadowColor)
                .assertNoPixelColorEquals(expectedExpandedShadowColor)
        }

        fun SemanticsNodeInteractionsProvider.assertMediumOverrideIsDisplayed() {
            onNodeWithTag("stack")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedMediumBorderWidth,
                    expectedBorderColor = expectedMediumBorderColor,
                    expectedBackgroundColor = expectedMediumBackgroundColor,
                )
            // Assert the shadow on the parent, because it is drawn outside of the stack's bounding box.
            onNodeWithTag("parent")
                .assertIsDisplayed()
                // When the shadow is drawn, at least some pixels are the approximate color we're looking for.
                .assertApproximatePixelColorPercentage(expectedMediumShadowColor, threshold = 0.1f) { it > 0f }
                .assertNoPixelColorEquals(expectedCompactShadowColor)
                .assertNoPixelColorEquals(expectedExpandedShadowColor)
        }

        fun SemanticsNodeInteractionsProvider.assertExpandedOverrideIsDisplayed() {
            onNodeWithTag("stack")
                .assertIsDisplayed()
                .assertRectangularBorderColor(
                    borderWidth = expectedExpandedBorderWidth,
                    expectedBorderColor = expectedExpandedBorderColor,
                    expectedBackgroundColor = expectedExpandedBackgroundColor,
                )
            // Assert the shadow on the parent, because it is drawn outside of the stack's bounding box.
            onNodeWithTag("parent")
                .assertIsDisplayed()
                // When the shadow is drawn, at least some pixels are the approximate color we're looking for.
                .assertApproximatePixelColorPercentage(expectedExpandedShadowColor, threshold = 0.1f) { it > 0f }
                .assertNoPixelColorEquals(expectedCompactShadowColor)
                .assertNoPixelColorEquals(expectedMediumShadowColor)
        }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @RunWith(AndroidJUnit4::class)
    class WithActivityRecreationTests {

        @get:Rule(order = 1)
        val addActivityToRobolectricRule = object : TestWatcher() {
            override fun starting(description: Description?) {
                super.starting(description)
                val appContext: Application = getApplicationContext()
                val activityInfo = ActivityInfo().apply {
                    name = TestActivity::class.java.name
                    packageName = appContext.packageName
                }
                shadowOf(appContext.packageManager).addOrUpdateActivity(activityInfo)
            }
        }

        @get:Rule(order = 2)
        internal val composeTestRule = createAndroidComposeRule<TestActivity>()

        @Config(qualifiers = "w${MAX_WIDTH_COMPACT}dp-h600dp")
        @Test
        fun `Should use the window size overrides with Activity recreation`(): Unit = with(composeTestRule) {
            assertCompactOverrideIsDisplayed()

            // Recreate the activity with a new window size
            RuntimeEnvironment.setQualifiers("w${MAX_WIDTH_MEDIUM}dp-h800dp")
            assertMediumOverrideIsDisplayed()

            // Recreate the activity with a new window size
            RuntimeEnvironment.setQualifiers("w${MIN_WIDTH_EXPANDED}dp-h1000dp")
            assertExpandedOverrideIsDisplayed()
        }

        internal class TestActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent { content() }
            }
        }
    }

    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    @RunWith(AndroidJUnit4::class)
    class WithoutActivityRecreationTests {

        @get:Rule
        val composeTestRule = createComposeRule()

        @Test
        fun `Should use the window size overrides without Activity recreation`(): Unit = with(composeTestRule) {
            windowChangingTest(
                arrange = { },
                act = { content() },
                assert = { windowSizeController ->
                    // Resize the window without recreating the Activity
                    windowSizeController.setWindowSizeInexact(width = (MAX_WIDTH_COMPACT - 100).dp, height = 600.dp)
                    assertCompactOverrideIsDisplayed()

                    // Resize the window without recreating the Activity
                    windowSizeController.setWindowSizeInexact(width = (MAX_WIDTH_MEDIUM - 100).dp, height = 800.dp)
                    assertMediumOverrideIsDisplayed()

                    // Resize the window without recreating the Activity
                    windowSizeController.setWindowSizeInexact(width = (MIN_WIDTH_EXPANDED + 100).dp, height = 1000.dp)
                    assertExpandedOverrideIsDisplayed()
                }
            )
        }
    }
}
