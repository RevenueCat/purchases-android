package com.revenuecat.purchases.ui.revenuecatui.components.iconcomponent

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.request.SuccessResult
import com.revenuecat.purchases.paywalls.components.IconComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.MaskShape
import com.revenuecat.purchases.paywalls.components.properties.Padding
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.previewEmptyState
import com.revenuecat.purchases.ui.revenuecatui.components.previewIconComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyles
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.ProvidePreviewImageLoader
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
@RunWith(AndroidJUnit4::class)
class IconComponentViewTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Fixed close icon keeps its size when margins overflow a fixed parent`() {
        val states = listOf(4.0, 16.0, 24.0).map { margin ->
            val icon = IconComponent(
                baseUrl = "https://example.com",
                iconName = "x",
                formats = IconComponent.Formats(webp = "x.webp"),
                size = Size(Fixed(40u), Fixed(40u)),
                color = ColorScheme(light = ColorInfo.Hex(Color.White.toArgb())),
                margin = Padding(margin, margin, margin, margin),
                iconBackground = IconComponent.IconBackground(
                    color = ColorScheme(light = ColorInfo.Hex(Color.Blue.toArgb())),
                    shape = MaskShape.Circle,
                ),
            )
            FakePaywallState(
                components = listOf(
                    StackComponent(
                        size = Size(Fixed(48u), Fixed(48u)),
                        backgroundColor = ColorScheme(light = ColorInfo.Hex(Color.LightGray.toArgb())),
                        components = listOf(StackComponent(components = listOf(icon))),
                    ),
                ),
            )
        }
        val imageLoader = closeIconImageLoader()

        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvidePreviewImageLoader(imageLoader) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        states.forEachIndexed { index, state ->
                            val style = (state.stack as StackComponentStyle).children.single() as StackComponentStyle
                            StackComponentView(
                                style = style,
                                state = state,
                                clickHandler = {},
                                modifier = Modifier.testTag("cell-$index"),
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        states.indices.forEach { index ->
            assertBlueBounds(left = 4.dp, top = 4.dp, right = 44.dp, bottom = 44.dp, tag = "cell-$index")
        }
    }

    @Test
    fun `Fixed width survives overflow while Fill height respects available space`() {
        renderConstrainedIcon(size = Size(Fixed(40u), Fill()))

        assertBlueBounds(left = 28.dp, top = 40.dp, right = 68.dp, bottom = 56.dp)
    }

    @Test
    fun `Fixed height survives overflow while Fill width respects available space`() {
        renderConstrainedIcon(size = Size(Fill(), Fixed(40u)))

        assertBlueBounds(left = 40.dp, top = 28.dp, right = 56.dp, bottom = 68.dp)
    }

    @Test
    fun `Asymmetric margins preserve their offset while a fixed icon overflows`() {
        renderConstrainedIcon(
            size = Size(Fixed(40u), Fixed(40u)),
            margin = PaddingValues(start = 32.dp, top = 16.dp, end = 0.dp, bottom = 16.dp),
        )

        assertBlueBounds(left = 44.dp, top = 28.dp, right = 84.dp, bottom = 68.dp)
    }

    private fun renderConstrainedIcon(size: Size, margin: PaddingValues = PaddingValues(16.dp)) {
        val imageLoader = closeIconImageLoader()
        composeTestRule.setContent {
            CompositionLocalProvider(LocalInspectionMode provides true) {
                ProvidePreviewImageLoader(imageLoader) {
                    Box(
                        Modifier.requiredSize(96.dp).background(Color.White).testTag("canvas").padding(24.dp),
                    ) {
                        Box(Modifier.requiredSize(48.dp).background(Color.LightGray)) {
                            IconComponentView(
                                style = previewIconComponentStyle(
                                    size = size,
                                    color = ColorStyles(light = ColorStyle.Solid(Color.White)),
                                    backgroundColor = ColorStyles(light = ColorStyle.Solid(Color.Blue)),
                                    paddingValues = PaddingValues(0.dp),
                                    marginValues = margin,
                                    border = null,
                                    shadow = null,
                                    shape = MaskShape.Rectangle(),
                                ),
                                state = previewEmptyState(),
                            )
                        }
                    }
                }
            }
        }
        composeTestRule.waitForIdle()
    }

    private fun assertBlueBounds(left: Dp, top: Dp, right: Dp, bottom: Dp, tag: String = "canvas") {
        val bitmap = composeTestRule.onNodeWithTag(tag).captureToImageCompat().asAndroidBitmap()
        val bounds = Rect(bitmap.width, bitmap.height, 0, 0)
        for (x in 0 until bitmap.width) {
            for (y in 0 until bitmap.height) {
                val color = Color(bitmap.getPixel(x, y))
                if (color.blue > color.red + 0.1f && color.blue > color.green + 0.1f) {
                    bounds.left = minOf(bounds.left, x)
                    bounds.top = minOf(bounds.top, y)
                    bounds.right = maxOf(bounds.right, x + 1)
                    bounds.bottom = maxOf(bounds.bottom, y + 1)
                }
            }
        }
        val expected = with(composeTestRule.density) {
            Rect(left.roundToPx(), top.roundToPx(), right.roundToPx(), bottom.roundToPx())
        }
        assertThat(bounds).describedAs("Icon bounds in $tag").isEqualTo(expected)
    }

    private fun closeIconImageLoader(): ImageLoader {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val bitmap = Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.White.toArgb()
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
        }
        Canvas(bitmap).apply {
            drawLine(30f, 30f, 70f, 70f, paint)
            drawLine(70f, 30f, 30f, 70f, paint)
        }
        return ImageLoader.Builder(context)
            .components {
                add { chain ->
                    SuccessResult(
                        drawable = BitmapDrawable(context.resources, bitmap),
                        request = chain.request,
                        dataSource = DataSource.MEMORY,
                    )
                }
            }
            .build()
    }
}
