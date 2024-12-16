package com.revenuecat.purchases.ui.revenuecatui.components

import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.ComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsConfig
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
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

@OptIn(ExperimentalCoilApi::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class LoadedPaywallComponentsTests {

    private enum class TestUrl(val urlString: String, val color: Color) {
        Blue(urlString = "https://blue", color = Color.Blue),
        Red(urlString = "https://red", color = Color.Red),
        ;
    }

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
    fun `Should change background color based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightColor = Color.Red
        val expectedDarkColor = Color.Yellow

        val paywallComponents = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = emptyList()),
                    background = Background.Color(
                        ColorScheme(
                            light = ColorInfo.Hex(expectedLightColor.toArgb()),
                            dark = ColorInfo.Hex(expectedDarkColor.toArgb()),
                        ),
                    ),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = paywallComponents,
        )

        themeChangingTest(
            arrange = { },
            act = { LoadedPaywallComponents(state = PaywallState.Loaded.Components(offering, paywallComponents)) },
            assert = { theme ->
                theme.setLight()
                onRoot()
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedLightColor)

                theme.setDark()
                onRoot()
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedDarkColor)
            }
        )
    }

    @Test
    fun `Should change background image based on theme`(): Unit = with(composeTestRule) {
        // Arrange
        val expectedLightImage = TestUrl.Red
        val expectedDarkImage = TestUrl.Blue

        val paywallComponents = PaywallComponentsData(
            templateName = "template",
            assetBaseURL = URL("https://assets.pawwalls.com"),
            componentsConfig = ComponentsConfig(
                base = PaywallComponentsConfig(
                    stack = StackComponent(components = emptyList()),
                    background = Background.Image(
                        ThemeImageUrls(
                            light = expectedLightImage.toImageUrls(),
                            dark = expectedDarkImage.toImageUrls(),
                        ),
                    ),
                    stickyFooter = null,
                ),
            ),
            componentsLocalizations = mapOf(LocaleId("en_US") to emptyMap()),
            defaultLocaleIdentifier = LocaleId("en_US"),
        )
        val offering = Offering(
            identifier = "identifier",
            serverDescription = "serverDescription",
            metadata = emptyMap(),
            availablePackages = emptyList(),
            paywallComponents = paywallComponents,
        )

        themeChangingTest(
            arrange = { },
            act = { LoadedPaywallComponents(state = PaywallState.Loaded.Components(offering, paywallComponents)) },
            assert = { theme ->
                theme.setLight()
                onRoot()
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedLightImage.color)

                theme.setDark()
                onRoot()
                    .assertIsDisplayed()
                    .assertPixelColorEquals(expectedDarkImage.color)
            }
        )
    }

    private fun FakeImageLoaderEngine.Builder.interceptAllTestUrls(): FakeImageLoaderEngine.Builder = apply {
        TestUrl.values().forEach { testUrl ->
            intercept(testUrl.urlString, ColorDrawable(testUrl.color.toArgb()))
        }
    }

    private fun TestUrl.toImageUrls(size: Int? = null): ImageUrls =
        ImageUrls(
            original = URL(urlString),
            webp = URL(urlString),
            webpLowRes = URL(urlString),
            width = size?.toUInt(),
            height = size?.toUInt(),
        )

}
