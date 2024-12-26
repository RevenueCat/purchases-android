package com.revenuecat.purchases.ui.revenuecatui.components.image

import android.app.Application
import android.content.pm.ActivityInfo
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
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
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentConditions
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertNoPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.windowChangingTest
import org.junit.After
import org.junit.Before
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
import java.net.URL

@OptIn(ExperimentalCoilApi::class)
@RunWith(Enclosed::class)
internal class ImageComponentViewWindowTests {

    private enum class TestUrl(val urlString: String, val color: Color) {
        Blue(urlString = "https://blue", color = Color.Blue),
        Red(urlString = "https://red", color = Color.Red),
        Yellow(urlString = "https://yellow", color = Color.Yellow),
        Green(urlString = "https://green", color = Color.Green),
        ;
    }

    private companion object {
        // The following values are taken from WindowWidthSizeClass.kt (minus or plus 1).
        private const val MAX_WIDTH_COMPACT = 599
        private const val MAX_WIDTH_MEDIUM = 839
        private const val MIN_WIDTH_EXPANDED = 841

        private const val SIZE_COMPONENT = 100u
        private const val SIZE_IMAGE = 1000
        private val expectedCompactColor = TestUrl.Blue
        private val expectedMediumColor = TestUrl.Red
        private val expectedExpandedColor = TestUrl.Yellow
        private val unexpectedColor = TestUrl.Green

        val component = ImageComponent(
            source = ThemeImageUrls(unexpectedColor.toImageUrls(SIZE_IMAGE)),
            size = Size(width = Fixed(SIZE_COMPONENT), height = Fixed(SIZE_COMPONENT)),
            overrides = ComponentOverrides(
                conditions = ComponentConditions(
                    compact = PartialImageComponent(
                        source = ThemeImageUrls(expectedCompactColor.toImageUrls(SIZE_IMAGE))
                    ),
                    medium = PartialImageComponent(
                        source = ThemeImageUrls(expectedMediumColor.toImageUrls(SIZE_IMAGE))
                    ),
                    expanded = PartialImageComponent(
                        source = ThemeImageUrls(expectedExpandedColor.toImageUrls(SIZE_IMAGE))
                    ),
                )
            )
        )
        val state = FakePaywallState(component)
        val styleFactory = StyleFactory(
            nonEmptyMapOf(
                LocaleId("en_US") to nonEmptyMapOf(
                    LocalizationKey("dummyKey") to LocalizationData.Text("dummyText")
                )
            )
        )
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle
        val content = @Composable { ImageComponentView(style, state, modifier = Modifier.testTag("image")) }

        fun SemanticsNodeInteractionsProvider.assertCompactOverrideIsDisplayed() {
            onNodeWithTag("image")
                .assertIsDisplayed()
                .assertPixelColorEquals(expectedCompactColor.color)
                .assertNoPixelColorEquals(expectedMediumColor.color)
                .assertNoPixelColorEquals(expectedExpandedColor.color)
        }

        fun SemanticsNodeInteractionsProvider.assertMediumOverrideIsDisplayed() {
            onNodeWithTag("image")
                .assertIsDisplayed()
                .assertPixelColorEquals(expectedMediumColor.color)
                .assertNoPixelColorEquals(expectedCompactColor.color)
                .assertNoPixelColorEquals(expectedExpandedColor.color)
        }

        fun SemanticsNodeInteractionsProvider.assertExpandedOverrideIsDisplayed() {
            onNodeWithTag("image")
                .assertIsDisplayed()
                .assertPixelColorEquals(expectedExpandedColor.color)
                .assertNoPixelColorEquals(expectedCompactColor.color)
                .assertNoPixelColorEquals(expectedMediumColor.color)
        }

        private fun FakeImageLoaderEngine.Builder.interceptAllTestUrls(): FakeImageLoaderEngine.Builder = apply {
            TestUrl.values().forEach { testUrl ->
                intercept(testUrl.urlString, ColorDrawable(testUrl.color.toArgb()))
            }
        }

        private fun TestUrl.toImageUrls(size: Int): ImageUrls =
            ImageUrls(
                original = URL(urlString),
                webp = URL(urlString),
                webpLowRes = URL(urlString),
                width = size.toUInt(),
                height = size.toUInt(),
            )
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

        @Before
        fun setup() {
            val engine = FakeImageLoaderEngine.Builder()
                .interceptAllTestUrls()
                .build()
            val imageLoader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext)
                .components { add(engine) }
                .build()

            Coil.setImageLoader(imageLoader)
        }

        @After
        fun teardown() {
            Coil.reset()
        }

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

        @Before
        fun setup() {
            val engine = FakeImageLoaderEngine.Builder()
                .interceptAllTestUrls()
                .build()
            val imageLoader = ImageLoader.Builder(InstrumentationRegistry.getInstrumentation().targetContext)
                .components { add(engine) }
                .build()

            Coil.setImageLoader(imageLoader)
        }

        @After
        fun teardown() {
            Coil.reset()
        }

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
