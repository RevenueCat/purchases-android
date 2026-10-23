package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.background
import com.revenuecat.purchases.ui.revenuecatui.components.properties.BackgroundStyle
import com.revenuecat.purchases.ui.revenuecatui.components.properties.ColorStyle
import com.revenuecat.purchases.ui.revenuecatui.helpers.FakePaywallState
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Regression tests for the overlapping sticky-footer layout. The footer is drawn on top of the
 * full-height scrollable content, which reserves bottom clearance equal to the footer's measured
 * height ([Modifier.footerBottomPadding]) so the last content item can scroll clear of the footer.
 *
 * The root invariants guarded here:
 * - A background (image or color) must NOT change the size of the element it decorates. An image
 *   background applied via [Modifier.background] previously used `paint(sizeToIntrinsics = true)`,
 *   so the painter's intrinsic size inflated the container.
 * - When the footer changes height after its first layout, the scrollable body must update its
 *   bottom clearance so its final item can still scroll above the footer.
 */
@RunWith(AndroidJUnit4::class)
internal class OverlappingFooterLayoutTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * A painter with a large intrinsic size, standing in for a loaded image background (e.g. the
     * reported 512x512 footer image). If the intrinsic size leaks into layout, the decorated element
     * grows far beyond its content.
     */
    private val largeIntrinsicPainter = object : Painter() {
        override val intrinsicSize: Size = Size(512f, 512f)
        override fun DrawScope.onDraw() = Unit
    }

    @Test
    fun `image background does not affect measured size`(): Unit =
        with(composeTestRule) {
            setContent {
                Row {
                    // Baseline: a color background never affects size.
                    Box(
                        modifier = Modifier
                            .testTag("color")
                            .background(BackgroundStyle.Color(ColorStyle.Solid(Color.Green))),
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 40.dp))
                    }
                    // An image background must behave identically: draw behind, never resize.
                    Box(
                        modifier = Modifier
                            .testTag("image")
                            .background(
                                BackgroundStyle.Image(
                                    painter = largeIntrinsicPainter,
                                    contentScale = ContentScale.Crop,
                                    colorOverlay = null,
                                ),
                            ),
                    ) {
                        Box(modifier = Modifier.size(width = 80.dp, height = 40.dp))
                    }
                }
            }
            waitForIdle()

            val colorSize = onNodeWithTag("color").fetchSemanticsNode().size
            val imageSize = onNodeWithTag("image").fetchSemanticsNode().size

            // The image-backed box measures to its content, exactly like the color-backed box. Before
            // the fix it ballooned toward the painter's (Crop-scaled) 512px intrinsic height.
            assertThat(imageSize.height).isEqualTo(colorSize.height)
            assertThat(imageSize.width).isEqualTo(colorSize.width)
        }

    @Test
    fun `last scroll item clears a footer whose height changes`(): Unit =
        with(composeTestRule) {
            val state = FakePaywallState()
            var footerHeight by mutableStateOf(60.dp)
            lateinit var scrollState: ScrollState

            setContent {
                scrollState = rememberScrollState()
                Box(Modifier.size(width = 300.dp, height = 400.dp)) {
                    PaywallComponentsScaffold(
                        state = state,
                        background = null,
                        footerContent = {
                            Box(
                                Modifier
                                    .testTag("footer")
                                    .fillMaxWidth()
                                    .height(footerHeight),
                            )
                        },
                    ) {
                        Column(
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(scrollState)
                                .footerBottomPadding(state),
                        ) {
                            Spacer(Modifier.height(600.dp))
                            Box(
                                Modifier
                                    .testTag("last-item")
                                    .fillMaxWidth()
                                    .height(40.dp),
                            )
                        }
                    }
                }
            }

            fun scrollToEndAndAssertLastItemClearsFooter() {
                runOnIdle { runBlocking { scrollState.scrollTo(scrollState.maxValue) } }
                waitForIdle()

                val lastItemBottom = onNodeWithTag("last-item").fetchSemanticsNode().boundsInRoot.bottom
                val footerTop = onNodeWithTag("footer").fetchSemanticsNode().boundsInRoot.top
                assertThat(lastItemBottom).isEqualTo(footerTop)
            }

            scrollToEndAndAssertLastItemClearsFooter()

            runOnIdle { footerHeight = 120.dp }
            scrollToEndAndAssertLastItemClearsFooter()
        }
}
