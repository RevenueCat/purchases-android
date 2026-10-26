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
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.TextComponent
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.TwoDimensionalAlignment
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import com.revenuecat.purchases.ui.revenuecatui.helpers.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.helpers.getOrThrow
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyMapOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
class StackComponentViewInsetsTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val text = "dummyText"
    private val textKey = LocalizationKey("dummyKey")
    private val localizations = nonEmptyMapOf(
        LocaleId("en_US") to nonEmptyMapOf(textKey to LocalizationData.Text(text)),
    )
    private val styleFactory = StyleFactory(localizations = localizations)

    // The root ZLayer of a paywall with a hero image offsets its other children below the status bar. The
    // flag that enables this is set directly below, so the hero itself is not needed.
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

    /**
     * Composes the stack only after the window has reported a status bar inset, like a paywall that loads
     * into an already visible window, and reads where its text landed after a single frame.
     */
    private fun ComposeContentTestRule.textTopOnFirstFrame(statusBarConsumedAbove: Boolean): Int {
        mainClock.autoAdvance = false
        val style = styleFactory.create(component).getOrThrow().componentStyle as StackComponentStyle
        val state = FakePaywallState(components = listOf(component))
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
                    StackComponentView(
                        style = style.copy(applyTopWindowInsets = true),
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
