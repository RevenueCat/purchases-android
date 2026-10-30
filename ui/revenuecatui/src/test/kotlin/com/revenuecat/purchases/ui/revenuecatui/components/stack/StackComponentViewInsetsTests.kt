package com.revenuecat.purchases.ui.revenuecatui.components.stack

import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.ImageComponent
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Badge
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.ImageUrls
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.paywalls.components.properties.VerticalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.resolveHeroInsets
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.net.URL
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class StackComponentViewInsetsTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val text = "dummyText"
    private val textKey = LocalizationKey("dummyKey")
    private val innerTextKey = LocalizationKey("innerKey")
    private val localizations = nonEmptyMapOf(
        LocaleId("en_US") to nonEmptyMapOf(
            textKey to LocalizationData.Text(text),
            innerTextKey to LocalizationData.Text("innerText"),
        ),
    )
    private val styleFactory = StyleFactory(localizations = localizations)

    private val component = StackComponent(
        components = listOf(
            TextComponent(text = textKey, color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb()))),
        ),
        dimension = Dimension.ZLayer(alignment = TwoDimensionalAlignment.TOP),
    )

    @Test
    fun `offsets children below the status bar on the first frame`(): Unit = with(composeTestRule) {
        val textTopPx = textTopOnFirstFrame(statusBarConsumedAbove = false)

        assertThat(textTopPx).isEqualTo(STATUS_BAR_PX)
    }

    @Test
    fun `does not offset children again on the first frame when an ancestor consumed the status bar`(): Unit =
        with(composeTestRule) {
            val textTopPx = textTopOnFirstFrame(statusBarConsumedAbove = true)

            // Only the ancestor's own padding, nothing added by the stack.
            assertThat(textTopPx).isEqualTo(STATUS_BAR_PX)
        }

    @Test
    fun `hero overlay badge applies the top safe area once for a long edge`() {
        assertEdgeBadgeInsets(TwoDimensionalAlignment.TOP)
    }

    @Test
    fun `hero overlay badge applies the top safe area once for a corner`() {
        assertEdgeBadgeInsets(TwoDimensionalAlignment.TOP_LEADING)
    }

    @Test
    fun `nested hero preserves overlay clearance at every ancestor ZLayer`() {
        val nestedHero = StackComponent(
            components = listOf(hero(), TextComponent(
                text = innerTextKey, color = ColorScheme(light = ColorInfo.Hex(Color.Black.toArgb())),
            )),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )
        val hiddenSide = StackComponent(
            visible = false,
            components = listOf(hero(width = SizeConstraint.Fixed(120u))),
        )
        val root = StackComponent(
            components = listOf(hiddenSide, StackComponent(components = listOf(nestedHero)), component),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )
        assertThat(composeTestRule.textTopOnFirstFrame(false, root)).isEqualTo(STATUS_BAR_PX)
        val innerTop = composeTestRule.onNodeWithText("innerText").getUnclippedBoundsInRoot().top
        assertThat(with(composeTestRule.density) { innerTop.roundToPx() }).isEqualTo(STATUS_BAR_PX)
    }

    @Test
    fun `unrelated nested card below a hero does not gain safe area padding`() {
        val root = StackComponent(components = listOf(
            hero(),
            StackComponent(
                components = listOf(hero(), component),
                dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            ),
        ))
        val expectedTop = with(composeTestRule.density) { 240.dp.roundToPx() }
        assertThat(composeTestRule.textTopOnFirstFrame(false, root)).isEqualTo(expectedTop)
    }

    @Test
    fun `top badge on a card below the hero does not gain safe area padding`() {
        val card = StackComponent(
            components = emptyList(),
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(160u)),
            badge = Badge(stack = component, style = Badge.Style.EdgeToEdge, alignment = TwoDimensionalAlignment.TOP),
        )
        val root = StackComponent(components = listOf(hero(), card))
        val expectedTop = with(composeTestRule.density) { 240.dp.roundToPx() }
        assertThat(composeTestRule.textTopOnFirstFrame(false, root)).isEqualTo(expectedTop)
    }

    @Test
    fun `top badge on hero stack retains its status bar clearance`() {
        val root = StackComponent(
            components = listOf(hero()),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
            badge = Badge(stack = component, style = Badge.Style.EdgeToEdge, alignment = TwoDimensionalAlignment.TOP),
        )
        assertThat(composeTestRule.textTopOnFirstFrame(false, root)).isEqualTo(STATUS_BAR_PX)
    }

    @Test
    fun `ancestor clearance is consumed before measuring a badge on a nested overlay`() {
        assertEdgeBadgeInsets(TwoDimensionalAlignment.TOP, statusBarConsumedAbove = true)
    }

    @Test
    fun `horizontal image with a visible sibling keeps text below the status bar`() {
        val root = StackComponent(
            components = listOf(hero(), component),
            dimension = Dimension.Horizontal(VerticalAlignment.TOP, FlexDistribution.START),
        )
        assertThat(composeTestRule.textTopOnFirstFrame(false, root)).isEqualTo(STATUS_BAR_PX)
    }

    private fun hero(width: SizeConstraint = SizeConstraint.Fill()): ImageComponent {
        val imageUrl = URL("https://example.com/hero.png")
        return ImageComponent(
            source = ThemeImageUrls(light = ImageUrls(
                original = imageUrl, webp = imageUrl, webpLowRes = imageUrl, width = 400u, height = 240u,
            )),
            size = Size(width, SizeConstraint.Fixed(240u)),
        )
    }

    private fun assertEdgeBadgeInsets(
        alignment: TwoDimensionalAlignment,
        statusBarConsumedAbove: Boolean = false,
    ) = with(composeTestRule) {
        val overlay = StackComponent(
            components = emptyList(),
            size = Size(SizeConstraint.Fill(), SizeConstraint.Fixed(160u)),
            badge = Badge(stack = component, style = Badge.Style.EdgeToEdge, alignment = alignment),
        )
        val root = StackComponent(
            components = listOf(hero(), overlay),
            dimension = Dimension.ZLayer(TwoDimensionalAlignment.TOP),
        )
        val textTopPx = textTopOnFirstFrame(
            statusBarConsumedAbove = statusBarConsumedAbove,
            componentUnderTest = root,
        )
        assertThat(textTopPx).isEqualTo(STATUS_BAR_PX)
    }

    /**
     * Composes the stack only after the window has reported a status bar inset, like a paywall that loads
     * into an already visible window, and reads where its text landed after a single frame.
     */
    private fun ComposeContentTestRule.textTopOnFirstFrame(
        statusBarConsumedAbove: Boolean,
        componentUnderTest: StackComponent = component,
    ): Int {
        mainClock.autoAdvance = false
        val style = styleFactory.create(componentUnderTest).getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState(components = listOf(componentUnderTest))
        var showStack by mutableStateOf(false)
        lateinit var view: View
        var density = 1f
        setContent {
            view = LocalView.current
            density = LocalDensity.current.density
            // Reading the insets installs Compose's insets listener on the view, so the dispatch below lands.
            WindowInsets.safeDrawing
            if (showStack) {
                val stack: @Composable () -> Unit = {
                    resolveHeroInsets(style, state)
                    StackComponentView(
                        style = style,
                        state = state,
                        clickHandler = { },
                    )
                }
                if (statusBarConsumedAbove) {
                    Box(Modifier.windowInsetsPadding(WindowInsets.statusBars)) { stack() }
                } else {
                    stack()
                }
            }
        }
        waitForIdle()

        runOnUiThread {
            val insets = WindowInsetsCompat.Builder()
                .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, STATUS_BAR_PX, 0, 0))
                .build()
            ViewCompat.dispatchApplyWindowInsets(view, insets)
        }
        waitForIdle()

        runOnUiThread { showStack = true }
        // The state change reaches the recomposer through the main looper, so the stack shows up a frame or
        // two later. Its position is read on the first frame it is laid out, before any further frame.
        var framesAdvanced = 0
        while (onAllNodesWithText(text).fetchSemanticsNodes().isEmpty()) {
            check(framesAdvanced++ < MAX_FRAMES_TO_APPEAR) { "The stack never showed up" }
            mainClock.advanceTimeByFrame()
            waitForIdle()
        }

        return (onNodeWithText(text).getUnclippedBoundsInRoot().top.value * density).roundToInt()
    }

    private companion object {
        const val STATUS_BAR_PX = 100
        const val MAX_FRAMES_TO_APPEAR = 5
    }
}
