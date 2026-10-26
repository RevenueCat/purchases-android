package com.revenuecat.purchases.ui.revenuecatui.components.style

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.PartialStackComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.resolveHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.components.stack.StackComponentView
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowPixelCopy
import java.io.File
import java.net.URL

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(shadows = [ShadowPixelCopy::class], sdk = [26])
internal class HiddenHeroLayoutReproductionTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `PWENG-317 hidden wide-only sibling must not move visible mobile hero down`() {
        val urls = ThemeImageUrls(light = ImageUrls(
            original = URL("https://example.com/hero.png"),
            webp = URL("https://example.com/hero.webp"),
            webpLowRes = URL("https://example.com/hero-small.webp"),
            width = 400u, height = 240u,
        ))
        val image = ImageComponent(source = urls, size = Size(
            width = SizeConstraint.Fill(), height = SizeConstraint.Fixed(240u),
        ))
        val mobileHero = StackComponent(
            components = listOf(image),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            backgroundColor = ColorScheme(ColorInfo.Hex(Color.Red.toArgb())),
        )
        val side = StackComponent(
            components = listOf(image), visible = false,
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL, 800.0,
                )),
                properties = PartialStackComponent(visible = true),
            )),
        )
        fun style(withSide: Boolean) = StyleFactory().create(StackComponent(
            components = if (withSide) listOf(side, mobileHero) else listOf(mobileHero),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            size = Size(SizeConstraint.Fixed(400u), SizeConstraint.Fixed(800u)),
            backgroundColor = ColorScheme(ColorInfo.Hex(Color.White.toArgb())),
        )).getOrThrow().componentStyle as StackComponentStyle

        val baseline = style(false)
        val changed = style(true)
        val state = FakePaywallState()
        val withSide = mutableStateOf(false)
        val showContent = mutableStateOf(false)
        lateinit var view: View
        composeTestRule.setContent {
            view = LocalView.current
            WindowInsets.safeDrawing
            if (showContent.value) {
                val style = if (withSide.value) changed else baseline
                resolveHeroInsets(style, state)
                StackComponentView(style, state, { })
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.runOnUiThread {
            ViewCompat.dispatchApplyWindowInsets(view, WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, 100, 0, 0)).build())
        }
        composeTestRule.runOnIdle { showContent.value = true }
        val before = capture(view, "android-control.png")
        composeTestRule.runOnIdle { withSide.value = true }
        val after = capture(view, "android-hidden-side.png")
        val beforeTop = firstRedRow(before)
        val afterTop = firstRedRow(after)
        println("PWENG-317 mobile hero top: control=$beforeTop hidden-side=$afterTop pixels")
        assertThat(beforeTop).isZero()
        assertThat(afterTop).isEqualTo(beforeTop)
    }

    private fun capture(view: View, name: String): Bitmap {
        composeTestRule.waitForIdle()
        lateinit var bitmap: Bitmap
        composeTestRule.runOnUiThread {
            bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(Canvas(bitmap))
        }
        val file = File("build/hidden-hero-evidence", name).also { it.parentFile.mkdirs() }
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return bitmap
    }

    private fun firstRedRow(bitmap: Bitmap): Int = (0 until bitmap.height).first { y ->
        bitmap.getPixel(bitmap.width / 2, y) == Color.Red.toArgb()
    }
}
