package com.revenuecat.purchases.ui.revenuecatui.components.modifier

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.Coil
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberAsyncImagePainter
import coil.test.FakeImageLoaderEngine
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FitMode
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertNoPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorCount
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorEquals
import com.revenuecat.purchases.ui.revenuecatui.assertions.assertPixelColorPercentage
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.toContentScale
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.urlsForCurrentTheme
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
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

    private val imageSizePx = 100

    @Before
    fun setup() {
        val engine = FakeImageLoaderEngine.Builder()
            .interceptAllTestUrls(imageSizePx)
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
        val testUrl = TestUrl.Blue
        val expectedColor = testUrl.color
        val background = Background.Image(ThemeImageUrls(light = testUrl.toImageUrls(size = imageSizePx)))
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val sizeDp = with(LocalDensity.current) { imageSizePx.toDp() }

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
            .assertPixelColorEquals(
                startX = 0,
                startY = 0,
                width = imageSizePx,
                height = imageSizePx,
                color = expectedColor
            )
    }

    @Test
    fun `Should draw image background behind content`(): Unit = with(composeTestRule) {
        // Arrange
        val testUrl = TestUrl.Blue
        val expectedColor = testUrl.color
        val background = Background.Image(ThemeImageUrls(light = testUrl.toImageUrls(size = imageSizePx)))
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val sizeDp = with(LocalDensity.current) { imageSizePx.toDp() }

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
                width = imageSizePx,
                height = imageSizePx,
                color = expectedColor,
                // Text rendering might not be fully deterministic (e.g. due to anti aliasing, font settings, etc.) so
                // we're just verifying that the majority of the Composable shows the background, but not all of it.
                predicate = { percentage -> percentage in 0.6f..0.99f }
            )
    }

    @Test
    fun `Should draw image background overlay behind content`(): Unit = with(composeTestRule) {
        // Arrange
        val unexpectedColor = TestUrl.Blue
        val expectedColor = Color.Yellow
        val textColor = Color.Red
        val background = Background.Image(
            value = ThemeImageUrls(light = unexpectedColor.toImageUrls(size = imageSizePx)),
            fitMode = FitMode.FILL,
            colorOverlay = ColorScheme(ColorInfo.Hex(expectedColor.toArgb())),
        )
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val sizeDp = with(LocalDensity.current) { imageSizePx.toDp() }

            // Act
            Text(
                text = "Hello",
                modifier = Modifier
                    .requiredSize(sizeDp)
                    .background(backgroundStyle)
                    .semantics { testTag = "text" },
                color = textColor,
            )
        }

        // Assert
        onNodeWithTag("text")
            .assertIsDisplayed()
            // The overlay should cover the entire background, as it is fully opaque.
            .assertNoPixelColorEquals(unexpectedColor.color)
            .assertPixelColorPercentage(
                color = expectedColor,
                // Text rendering might not be fully deterministic (e.g. due to anti aliasing, font settings, etc.) so
                // we're just verifying that the majority of the Composable shows the overlay, but not all of it.
                predicate = { percentage -> percentage in 0.6f..0.99f }
            )
            .assertPixelColorCount(
                color = textColor,
                // The text should be drawn on top of the overlay.
                predicate = { count -> count > 0}
            )
    }

    @Test
    fun `Image background with FitMode FILL should fill the parent`(): Unit = with(composeTestRule) {
        // Arrange
        val testUrl = TestUrl.Blue
        val expectedColor = testUrl.color
        val unexpectedColor = Color.Yellow
        val background = Background.Image(
            value = ThemeImageUrls(light = testUrl.toImageUrls(size = imageSizePx)),
            fitMode = FitMode.FILL
        )
        setContent {
            val backgroundStyle = background.toBackgroundStyle()
            val imageSizeDp = with(LocalDensity.current) { imageSizePx.toDp() }

            // Act
            Box(
                modifier = Modifier
                    .requiredWidth(imageSizeDp * 3)
                    // Parent is taller than the image
                    .requiredHeight(imageSizeDp * 4)
                    .background(unexpectedColor)
                    .semantics { testTag = "parent" }
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundStyle)
                )
            }
        }

        // Assert
        onNodeWithTag("parent")
            .assertIsDisplayed()
            .assertPixelColorEquals(expectedColor)
            .assertNoPixelColorEquals(unexpectedColor)
    }

    private fun FakeImageLoaderEngine.Builder.interceptAllTestUrls(imageSizePx: Int): FakeImageLoaderEngine.Builder =
        apply {
            TestUrl.values().forEach { testUrl ->
                // Returning a BitmapDrawable instead of a ColorDrawable, as the latter does not have an intrinsic
                // size and is thus "automatically" resized. We'll have bitmaps in production, and we want to test
                // resizing and scaling, so we should use bitmaps in these tests too.
                intercept(
                    data = testUrl.urlString,
                    drawable = BitmapDrawable(
                        ApplicationProvider.getApplicationContext<Application>().resources,
                        testUrl.toBitmap(imageSizePx)
                    )
                )
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

    private fun TestUrl.toBitmap(size: Int): Bitmap =
        Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).also { bitmap ->
            Canvas(bitmap).apply {
                drawRect(
                    0f,
                    0f,
                    size.toFloat(),
                    size.toFloat(),
                    Paint().apply { color = this@toBitmap.color.toArgb() },
                )
            }
        }

    @JvmSynthetic
    @Composable
    internal fun Background.Image.toBackgroundStyle(colorOverlay: Color? = null): BackgroundStyle {
        val imageUrls = value.urlsForCurrentTheme
        val contentScale = fitMode.toContentScale()
        return BackgroundStyle.Image(
            painter = rememberAsyncImagePainter(
                model = imageUrls.webp.toString(),
                placeholder = rememberAsyncImagePainter(
                    model = imageUrls.webpLowRes.toString(),
                    error = null,
                    fallback = null,
                    contentScale = contentScale,
                ),
                error = null,
                fallback = null,
                contentScale = contentScale,
            ),
            contentScale = contentScale,
            colorOverlay = colorOverlay?.let { ColorStyle.Solid(it) }
        )
    }
}
