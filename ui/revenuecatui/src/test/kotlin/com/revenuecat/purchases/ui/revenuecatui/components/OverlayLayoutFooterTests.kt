package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.PartialStickyFooterComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.properties.ColorInfo
import com.revenuecat.purchases.paywalls.components.properties.Dimension
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.HorizontalAlignment
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.StackComponent
import com.revenuecat.purchases.paywalls.components.StickyFooterComponent
import com.revenuecat.purchases.paywalls.components.common.ComponentOverride
import com.revenuecat.purchases.ui.revenuecatui.helpers.captureToImageCompat
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowPixelCopy

@RunWith(AndroidJUnit4::class)
internal class OverlayLayoutFooterTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `trailing split reserves half the width`() = assertSplit(StickyFooterComponent.Layout.SPLIT_TRAILING)

    @Test
    fun `leading split reserves half the width`() = assertSplit(StickyFooterComponent.Layout.SPLIT_LEADING)

    @Test
    fun `trailing split follows RTL`() = assertSplit(StickyFooterComponent.Layout.SPLIT_TRAILING, LayoutDirection.Rtl)

    @Test
    fun `leading split follows RTL`() = assertSplit(StickyFooterComponent.Layout.SPLIT_LEADING, LayoutDirection.Rtl)

    @Test
    fun `hidden footer can omit its content without a missing measurable crash`() {
        val state = FakePaywallState(stickyFooter = StickyFooterComponent(
            StackComponent(emptyList()), layout = StickyFooterComponent.Layout.HIDDEN,
        ))
        composeTestRule.setContent {
            OverlayLayout(state, Modifier.fillMaxSize(), hasFooter = true) {
                Box(Modifier.fillMaxSize().testTag("body"))
                ComponentView(style = checkNotNull(state.stickyFooter), state = state, onClick = {})
            }
        }
        assertThat(composeTestRule.onNodeWithTag("body").fetchSemanticsNode().boundsInRoot.width).isGreaterThan(0f)
        assertThat(state.footerHeightPx).isZero()
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    @Config(shadows = [ShadowPixelCopy::class], sdk = [26])
    fun `split footer background fills the column even when its stack fits content`() {
        val state = FakePaywallState(stickyFooter = StickyFooterComponent(
            stack = StackComponent(
                components = listOf(StackComponent(
                    components = emptyList(),
                    size = Size(width = Fill(), height = Fixed(40u)),
                    background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.Green.toArgb()))),
                )),
                dimension = Dimension.Vertical(HorizontalAlignment.CENTER, FlexDistribution.CENTER),
                background = Background.Color(ColorScheme(light = ColorInfo.Hex(Color.Red.toArgb()))),
            ),
            layout = StickyFooterComponent.Layout.SPLIT_TRAILING,
        ))
        composeTestRule.setContent {
            OverlayLayout(state, Modifier.fillMaxSize(), hasFooter = true) {
                Box(Modifier.fillMaxSize().testTag("body"))
                ComponentView(
                    style = checkNotNull(state.stickyFooter), state = state, onClick = {},
                    modifier = Modifier.testTag("footer"),
                )
            }
        }
        val bodyBounds = composeTestRule.onNodeWithTag("body").fetchSemanticsNode().boundsInRoot
        val footer = composeTestRule.onNodeWithTag("footer")
        assertThat(footer.fetchSemanticsNode().boundsInRoot.height).isEqualTo(bodyBounds.height)
        val bitmap = footer.captureToImageCompat().asAndroidBitmap()
        assertThat(bitmap.getPixel(bitmap.width / 2, 1)).isEqualTo(Color.Red.toArgb())
        assertThat(bitmap.getPixel(bitmap.width / 2, bitmap.height - 2)).isEqualTo(Color.Red.toArgb())
        assertThat(bitmap.getPixel(bitmap.width / 2, bitmap.height / 2)).isEqualTo(Color.Green.toArgb())
    }

    private fun assertSplit(layout: StickyFooterComponent.Layout, direction: LayoutDirection = LayoutDirection.Ltr) {
        val state = FakePaywallState(stickyFooter = StickyFooterComponent(StackComponent(emptyList()), layout = layout))
        composeTestRule.setContent {
            CompositionLocalProvider(LocalLayoutDirection provides direction) {
                OverlayLayout(state, Modifier.fillMaxSize(), hasFooter = true) {
                    Box(Modifier.fillMaxSize().testTag("body"))
                    Box(Modifier.fillMaxWidth().height(60.dp).testTag("footer"))
                }
            }
        }
        val body = composeTestRule.onNodeWithTag("body").fetchSemanticsNode().boundsInRoot
        val footer = composeTestRule.onNodeWithTag("footer").fetchSemanticsNode().boundsInRoot
        assertThat(body.width).isEqualTo(footer.width)
        assertThat(body.height).isEqualTo(footer.height)
        val footerOnLeft = (layout == StickyFooterComponent.Layout.SPLIT_LEADING) == (direction == LayoutDirection.Ltr)
        assertThat(if (footerOnLeft) footer.right else body.right).isEqualTo(if (footerOnLeft) body.left else footer.left)
        assertThat(state.footerHeightPx).isZero()
    }

    @Test
    fun `viewport hiding removes reserved height and resets when resized`() {
        val state = FakePaywallState(stickyFooter = StickyFooterComponent(
            stack = StackComponent(emptyList()),
            overrides = listOf(ComponentOverride(
                conditions = listOf(ComponentOverride.Condition.WindowWidthRule(
                    operator = ComponentOverride.ComparisonOperator.GREATER_THAN_OR_EQUAL,
                    value = 700.0,
                )),
                properties = PartialStickyFooterComponent(layout = StickyFooterComponent.Layout.HIDDEN),
            )),
        ))
        state.paywallBoundsDp = DpSize(390.dp, 600.dp)
        composeTestRule.setContent {
            OverlayLayout(state, Modifier.fillMaxSize(), hasFooter = true) {
                Box(Modifier.fillMaxSize().testTag("body"))
                Box(Modifier.fillMaxWidth().height(60.dp).testTag("footer"))
            }
        }
        composeTestRule.runOnIdle {
            assertThat(state.footerHeightPx).isGreaterThan(0)
            state.paywallBoundsDp = DpSize(800.dp, 600.dp)
        }
        composeTestRule.onNodeWithTag("footer").assertIsNotDisplayed()
        composeTestRule.runOnIdle {
            assertThat(state.footerHeightPx).isZero()
            state.paywallBoundsDp = DpSize(390.dp, 600.dp)
        }
        composeTestRule.runOnIdle { assertThat(state.footerHeightPx).isGreaterThan(0) }
    }
}
