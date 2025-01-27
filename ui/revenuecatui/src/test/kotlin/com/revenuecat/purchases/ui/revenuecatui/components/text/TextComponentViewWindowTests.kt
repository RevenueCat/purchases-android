package com.revenuecat.purchases.ui.revenuecatui.components.text

import android.app.Application
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.paywalls.components.PartialTextComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentConditions
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertTextColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.components.style.TextComponentStyle
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
internal class TextComponentViewWindowTests {

    private companion object {
        // The following values are taken from WindowWidthSizeClass.kt (minus or plus 1).
        private const val MAX_WIDTH_COMPACT = 599
        private const val MAX_WIDTH_MEDIUM = 839
        private const val MIN_WIDTH_EXPANDED = 841

        private val localeId = LocaleId("en_US")
        private val defaultLocalizationKey = LocalizationKey("default key")
        private val compactLocalizationKey = LocalizationKey("compact key")
        private val mediumLocalizationKey = LocalizationKey("medium key")
        private val expandedLocalizationKey = LocalizationKey("expanded key")
        private const val UNEXPECTED_TEXT = "default text"
        private const val EXPECTED_TEXT_COMPACT = "compact text"
        private const val EXPECTED_TEXT_MEDIUM = "medium text"
        private const val EXPECTED_TEXT_EXPANDED = "expanded text"
        private val expectedCompactTextColor = Color.Black
        private val expectedMediumTextColor = Color.White
        private val expectedExpandedTextColor = Color.Blue
        private val expectedCompactBackgroundColor = Color.Yellow
        private val expectedMediumBackgroundColor = Color.Red
        private val expectedExpandedBackgroundColor = Color.Green
        private val localizations = nonEmptyMapOf(
            localeId to nonEmptyMapOf(
                defaultLocalizationKey to LocalizationData.Text(UNEXPECTED_TEXT),
                compactLocalizationKey to LocalizationData.Text(EXPECTED_TEXT_COMPACT),
                mediumLocalizationKey to LocalizationData.Text(EXPECTED_TEXT_MEDIUM),
                expandedLocalizationKey to LocalizationData.Text(EXPECTED_TEXT_EXPANDED),
            )
        )
        val component = TextComponent(
            text = defaultLocalizationKey,
            color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            overrides = ComponentOverrides(
                conditions = ComponentConditions(
                    compact = PartialTextComponent(
                        text = compactLocalizationKey,
                        color = ColorScheme(ColorInfo.Hex(expectedCompactTextColor.toArgb())),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedCompactBackgroundColor.toArgb())),
                    ),
                    medium = PartialTextComponent(
                        text = mediumLocalizationKey,
                        color = ColorScheme(ColorInfo.Hex(expectedMediumTextColor.toArgb())),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedMediumBackgroundColor.toArgb())),
                    ),
                    expanded = PartialTextComponent(
                        text = expandedLocalizationKey,
                        color = ColorScheme(ColorInfo.Hex(expectedExpandedTextColor.toArgb())),
                        backgroundColor = ColorScheme(ColorInfo.Hex(expectedExpandedBackgroundColor.toArgb())),
                    ),
                )
            )
        )
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeId,
            component
        )
        val styleFactory = StyleFactory(
            localizations = localizations,
            uiConfig = UiConfig(),
            fontAliases = emptyMap(),
            offering = Offering(
            identifier = "identifier",
            serverDescription = "description",
            metadata = emptyMap(),
            availablePackages = emptyList(),
        )
        )
        val style = styleFactory.create(component).getOrThrow() as TextComponentStyle
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
            // Assert that the compact override is displayed.
            onNodeWithText(UNEXPECTED_TEXT)
                .assertIsNotDisplayed()
            onNodeWithText(EXPECTED_TEXT_COMPACT)
                .assertIsDisplayed()
                .assertTextColorEquals(expectedCompactTextColor)
                .assertPixelColorPercentage(expectedCompactBackgroundColor) { percentage -> percentage > 0.4 }

            // Recreate the activity with a new window size.
            RuntimeEnvironment.setQualifiers("w${MAX_WIDTH_MEDIUM}dp-h800dp")
            // Assert that the medium override is displayed.
            onNodeWithText(EXPECTED_TEXT_COMPACT)
                .assertIsNotDisplayed()
            onNodeWithText(EXPECTED_TEXT_MEDIUM)
                .assertIsDisplayed()
                .assertTextColorEquals(expectedMediumTextColor)
                .assertPixelColorPercentage(expectedMediumBackgroundColor) { percentage -> percentage > 0.4 }

            // Recreate the activity with a new window size.
            RuntimeEnvironment.setQualifiers("w${MIN_WIDTH_EXPANDED}dp-h1000dp")
            // Assert that the expanded override is displayed.
            onNodeWithText(EXPECTED_TEXT_MEDIUM)
                .assertIsNotDisplayed()
            onNodeWithText(EXPECTED_TEXT_EXPANDED)
                .assertIsDisplayed()
                .assertTextColorEquals(expectedExpandedTextColor)
                .assertPixelColorPercentage(expectedExpandedBackgroundColor) { percentage -> percentage > 0.4 }
        }

        internal class TestActivity : ComponentActivity() {
            override fun onCreate(savedInstanceState: Bundle?) {
                super.onCreate(savedInstanceState)
                setContent { TextComponentView(style, state) }
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
                act = { TextComponentView(style, state) },
                assert = { windowSizeController ->
                    // Resize the window without recreating the Activity.
                    windowSizeController.setWindowSizeInexact(width = (MAX_WIDTH_COMPACT - 100).dp, height = 600.dp)
                    // Assert that the compact override is displayed.
                    onNodeWithText(UNEXPECTED_TEXT)
                        .assertIsNotDisplayed()
                    onNodeWithText(EXPECTED_TEXT_COMPACT)
                        .assertIsDisplayed()
                        .assertTextColorEquals(expectedCompactTextColor)
                        .assertPixelColorPercentage(expectedCompactBackgroundColor) { percentage -> percentage > 0.4 }

                    // Resize the window without recreating the Activity.
                    windowSizeController.setWindowSizeInexact(width = (MAX_WIDTH_MEDIUM - 100).dp, height = 800.dp)
                    // Assert that the medium override is displayed.
                    onNodeWithText(EXPECTED_TEXT_COMPACT)
                        .assertIsNotDisplayed()
                    onNodeWithText(EXPECTED_TEXT_MEDIUM)
                        .assertIsDisplayed()
                        .assertTextColorEquals(expectedMediumTextColor)
                        .assertPixelColorPercentage(expectedMediumBackgroundColor) { percentage -> percentage > 0.4 }

                    // Resize the window without recreating the Activity.
                    windowSizeController.setWindowSizeInexact(width = (MIN_WIDTH_EXPANDED + 100).dp, height = 1000.dp)
                    // Assert that the expanded override is displayed.
                    onNodeWithText(EXPECTED_TEXT_MEDIUM)
                        .assertIsNotDisplayed()
                    onNodeWithText(EXPECTED_TEXT_EXPANDED)
                        .assertIsDisplayed()
                        .assertTextColorEquals(expectedExpandedTextColor)
                        .assertPixelColorPercentage(expectedExpandedBackgroundColor) { percentage -> percentage > 0.4 }
                },
            )
        }
    }
}
