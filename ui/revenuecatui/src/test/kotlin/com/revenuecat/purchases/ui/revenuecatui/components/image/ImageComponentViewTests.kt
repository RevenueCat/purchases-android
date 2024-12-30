package com.revenuecat.purchases.ui.revenuecatui.components.image

import android.graphics.drawable.ColorDrawable
import android.os.LocaleList
import androidx.compose.material3.Switch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialImageComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverrides
import com.revenuecat.purchases.paywalls.components.common.ComponentStates
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toJavaLocale
import com.revenuecat.purchases.ui.revenuecatui.components.style.ImageComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import com.revenuecat.purchases.ui.revenuecatui.helpers.themeChangingTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy
import java.net.URL

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@OptIn(ExperimentalCoilApi::class)
@RunWith(AndroidJUnit4::class)
internal class ImageComponentViewTests {

    private enum class TestUrl(val urlString: String, val color: Color) {
        Blue(urlString = "https://blue", color = Color.Blue),
        Red(urlString = "https://red", color = Color.Red),
        Yellow(urlString = "https://yellow", color = Color.Yellow),
        Green(urlString = "https://green", color = Color.Green),
        ;
    }

    @get:Rule
    val composeTestRule = createComposeRule()

    private val localeIdEnUs = LocaleId("en_US")
    private val localeIdNlNl = LocaleId("nl_NL")
    private val defaultLocale = localeIdEnUs
    private val dummyLocalizations = nonEmptyMapOf(
        defaultLocale to nonEmptyMapOf(LocalizationKey("dummy") to LocalizationData.Text("dummy"))
    )
    private val sizeImagePx = 1000
    private val sizeComponentDp = Size(width = Fixed(100u), height = Fixed(100u))
    private val fitMode = FitMode.FILL

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
    fun `Should use the default image if no localized images set`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedColor = TestUrl.Blue
        val component = ImageComponent(
            source = ThemeImageUrls(expectedColor.toImageUrls(size = sizeImagePx)),
            size = sizeComponentDp,
            fitMode = fitMode,
        )
        val state = FakePaywallState(
            localizations = dummyLocalizations,
            defaultLocaleIdentifier = defaultLocale,
            component
        )
        val styleFactory = StyleFactory(dummyLocalizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle

        // Act
        setContent { ImageComponentView(style = style, state = state, modifier = Modifier.testTag("image")) }

        // Assert
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedColor.color)
    }

    @Test
    fun `Should change the image based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = TestUrl.Blue
        val expectedDarkColor = TestUrl.Red
        val component = ImageComponent(
            source = ThemeImageUrls(
                light = expectedLightColor.toImageUrls(size = sizeImagePx),
                dark = expectedDarkColor.toImageUrls(size = sizeImagePx),
            ),
            size = sizeComponentDp,
            fitMode = fitMode,
        )
        val state = FakePaywallState(
            localizations = dummyLocalizations,
            defaultLocaleIdentifier = defaultLocale,
            component
        )
        val styleFactory = StyleFactory(dummyLocalizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle

        // Act, Assert
        themeChangingTest(
            arrange = { },
            act = { ImageComponentView(style = style, state = state, modifier = Modifier.testTag("image")) },
            assert = { theme ->
                theme.setLight()
                onNodeWithTag("image")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedLightColor.color)

                theme.setDark()
                onNodeWithTag("image")
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedDarkColor.color)
            }
        )
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun `Should use the selected overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedUnselectedColor = TestUrl.Blue
        val expectedSelectedColor = TestUrl.Red
        val component = ImageComponent(
            source = ThemeImageUrls(expectedUnselectedColor.toImageUrls(size = sizeImagePx)),
            size = sizeComponentDp,
            fitMode = fitMode,
            overrides = ComponentOverrides(
                states = ComponentStates(
                    selected = PartialImageComponent(
                        source = ThemeImageUrls(expectedSelectedColor.toImageUrls(size = sizeImagePx)),
                    ),
                ),
            )
        )
        val state = FakePaywallState(
            localizations = dummyLocalizations,
            defaultLocaleIdentifier = defaultLocale,
            component
        )
        val styleFactory = StyleFactory(dummyLocalizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle

        // Act
        setContent {
            var selected by remember { mutableStateOf(false) }
            ImageComponentView(style = style, state = state, selected = selected, modifier = Modifier.testTag("image"))
            Switch(
                checked = selected,
                onCheckedChange = { selected = it },
                modifier = Modifier.testTag("switch")
            )
        }

        // Assert
        waitUntilExactlyOneExists(hasTestTag("image"))
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedUnselectedColor.color)

        // Change `selected` to true
        onNodeWithTag("switch")
            .performClick()

        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedSelectedColor.color)
    }

    @Test
    fun `Should use the intro offer overrides`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedIneligibleColor = TestUrl.Blue
        val expectedEligibleColor = TestUrl.Red
        val component = ImageComponent(
            source = ThemeImageUrls(expectedIneligibleColor.toImageUrls(size = sizeImagePx)),
            size = sizeComponentDp,
            fitMode = fitMode,
            overrides = ComponentOverrides(
                introOffer = PartialImageComponent(
                    source = ThemeImageUrls(expectedEligibleColor.toImageUrls(size = sizeImagePx)),
                ),
            )
        )
        val state = FakePaywallState(
            localizations = dummyLocalizations,
            defaultLocaleIdentifier = defaultLocale,
            component
        )
        val styleFactory = StyleFactory(dummyLocalizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle

        // Act
        setContent {
            ImageComponentView(
                style = style,
                state = state,
                modifier = Modifier.testTag("image")
            )
        }

        // Assert
        state.update(isEligibleForIntroOffer = false)
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedIneligibleColor.color)

        state.update(isEligibleForIntroOffer = true)
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedEligibleColor.color)
    }

    @Test
    fun `Should use the correct image when the locale changes`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedEnUsColor = TestUrl.Blue
        val expectedNlNlColor = TestUrl.Red
        val component = ImageComponent(
            source = ThemeImageUrls(expectedEnUsColor.toImageUrls(size = sizeImagePx)),
            size = sizeComponentDp,
            overrideSourceLid = LocalizationKey("image"),
            fitMode = fitMode,
        )
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                LocalizationKey("image") to LocalizationData.Image(
                    ThemeImageUrls(expectedEnUsColor.toImageUrls(size = sizeImagePx))
                ),
            ),
            localeIdNlNl to nonEmptyMapOf(
                LocalizationKey("image") to LocalizationData.Image(
                    ThemeImageUrls(expectedNlNlColor.toImageUrls(size = sizeImagePx))
                ),
            )
        )
        val styleFactory = StyleFactory(localizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        // Act
        setContent {
            ImageComponentView(
                style = style,
                state = state,
                modifier = Modifier.testTag("image")
            )
        }

        // Assert
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedEnUsColor.color)

        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedNlNlColor.color)
    }

    @Test
    fun `Should use the correct override image when the locale changes`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedEnUsDefaultColor = TestUrl.Blue
        val expectedEnUsSelectedColor = TestUrl.Red
        val expectedNlNlDefaultColor = TestUrl.Yellow
        val expectedNlNlSelectedColor = TestUrl.Green
        
        val component = ImageComponent(
            source = ThemeImageUrls(expectedEnUsDefaultColor.toImageUrls(size = sizeImagePx)),
            size = sizeComponentDp,
            overrideSourceLid = LocalizationKey("default_image"),
            fitMode = fitMode,
            overrides = ComponentOverrides(
                states = ComponentStates(
                    selected = PartialImageComponent(
                        source = ThemeImageUrls(expectedEnUsSelectedColor.toImageUrls(size = sizeImagePx)),
                        overrideSourceLid = LocalizationKey("selected_image"),
                    )
                )
            )
        )
        
        val localizations = nonEmptyMapOf(
            localeIdEnUs to nonEmptyMapOf(
                LocalizationKey("default_image") to LocalizationData.Image(
                    ThemeImageUrls(expectedEnUsDefaultColor.toImageUrls(size = sizeImagePx))
                ),
                LocalizationKey("selected_image") to LocalizationData.Image(
                    ThemeImageUrls(expectedEnUsSelectedColor.toImageUrls(size = sizeImagePx))
                )
            ),
            localeIdNlNl to nonEmptyMapOf(
                LocalizationKey("default_image") to LocalizationData.Image(
                    ThemeImageUrls(expectedNlNlDefaultColor.toImageUrls(size = sizeImagePx))
                ),
                LocalizationKey("selected_image") to LocalizationData.Image(
                    ThemeImageUrls(expectedNlNlSelectedColor.toImageUrls(size = sizeImagePx))
                )
            )
        )
        
        val styleFactory = StyleFactory(localizations)
        val style = styleFactory.create(component, { }).getOrThrow() as ImageComponentStyle
        val state = FakePaywallState(
            localizations = localizations,
            defaultLocaleIdentifier = localeIdEnUs,
            component
        )

        // Act
        setContent {
            var selected by remember { mutableStateOf(false) }
            ImageComponentView(
                style = style,
                state = state,
                selected = selected,
                modifier = Modifier.testTag("image")
            )
            Switch(
                checked = selected,
                onCheckedChange = { selected = it },
                modifier = Modifier.testTag("switch")
            )
        }

        // Assert
        // Change locale to en_US
        state.update(localeList = LocaleList(localeIdEnUs.toJavaLocale()))
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedEnUsDefaultColor.color)

        // Change `selected` to true
        onNodeWithTag("switch").performClick()
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedEnUsSelectedColor.color)

        // Change locale to nl_NL
        state.update(localeList = LocaleList(localeIdNlNl.toJavaLocale()))
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedNlNlSelectedColor.color)

        // Change `selected` to false
        onNodeWithTag("switch").performClick()
        onNodeWithTag("image")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedNlNlDefaultColor.color)
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
