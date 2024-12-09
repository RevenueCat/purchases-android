package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import android.graphics.drawable.ColorDrawable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.test.FakeImageLoaderEngine
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyle
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
class BackgroundTests {

    private enum class TestUrl(val urlString: String, val color: Color) {
        Blue(urlString = "https://blue", color = Color.Blue),
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
    fun `Should properly set an image background`(): Unit = with(composeTestRule) {
        // Arrange
        val sizePx = 100
        val testUrl = TestUrl.Blue
        val expectedColor = testUrl.color
        val background = Background.Image(ThemeImageUrls(light = testUrl.toImageUrls()))
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val sizeDp = with(LocalDensity.current) { sizePx.toDp() }

            // Act
            Box(
                modifier = Modifier
                    .requiredSize(sizeDp)
                    .background(backgroundStyle)
                    .semantics { testTag = "box" }
            )
        }

        // Assert
        onNodeWithTag("box")
            .assertIsDisplayed()
            .assertPixelColorEquals(startX = 0, startY = 0, width = sizePx, height = sizePx, color = expectedColor)
    }

    @Test
    fun `Should draw image background behind content`(): Unit = with(composeTestRule) {
        // Arrange
        val sizePx = 100
        val testUrl = TestUrl.Blue
        val expectedColor = testUrl.color
        val background = Background.Image(ThemeImageUrls(light = testUrl.toImageUrls()))
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val sizeDp = with(LocalDensity.current) { sizePx.toDp() }

            // Act
            Text(
                text = "Hello",
                modifier = Modifier
                    .requiredSize(sizeDp)
                    .background(backgroundStyle)
                    .semantics { testTag = "box" }
            )
        }

        // Assert
        onNodeWithTag("box")
            .assertIsDisplayed()
            .assertPixelColorPercentage(
                startX = 0,
                startY = 0,
                width = sizePx,
                height = sizePx,
                color = expectedColor,
                // Text rendering might not be fully deterministic (e.g. due to anti aliasing, font settings, etc.) so
                // we're just verifying that the majority of the Composable shows the background, but not all of it.
                predicate = { percentage -> percentage in 0.6f..0.99f }
            )
    }

    private fun FakeImageLoaderEngine.Builder.interceptAllTestUrls(): FakeImageLoaderEngine.Builder = apply {
        TestUrl.values().forEach { testUrl ->
            intercept(testUrl.urlString, ColorDrawable(testUrl.color.toArgb()))
        }
    }

    private fun TestUrl.toImageUrls(): ImageUrls =
        ImageUrls(
            original = URL(urlString),
            webp = URL(urlString),
            webpLowRes = URL(urlString),
            width = 100u,
            height = 100u,
        )
}
