package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.paywalls.components.properties.FlexDistribution
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fixed
import com.revenuecat.purchases.ui.revenuecatui.components.BoxWithUnboundedFillMinimums
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.ComponentSizeParentDataModifier
import com.revenuecat.purchases.ui.revenuecatui.components.modifier.size
import com.revenuecat.purchases.ui.revenuecatui.components.stack.ConstrainedFillLayout
import com.revenuecat.purchases.ui.revenuecatui.extensions.trackMainAxisUnbounded
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.ParameterizedRobolectricTestRunner

@RunWith(ParameterizedRobolectricTestRunner::class)
internal class WebViewSizeTest(private val horizontal: Boolean) {
    @get:Rule val rule = createComposeRule()

    @Test
    fun `Fill native child keeps the viewport size under scrolling`() {
        lateinit var child: View
        render(Size(Fill(), Fill())) { context ->
            FrameLayout(context).apply {
                child = View(context)
                addView(child, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
        rule.runOnIdle {
            with(rule.density) {
                assertThat(child.width).isEqualTo(200.dp.roundToPx())
                assertThat(child.height).isEqualTo(400.dp.roundToPx())
            }
        }
    }

    @Test
    fun `Fit native view can grow past the viewport`() {
        lateinit var view: View
        render(if (horizontal) Size(Fit(), Fill()) else Size(Fill(), Fit())) { context ->
            View(context).also {
                view = it
                it.layoutParams = ViewGroup.LayoutParams(
                    if (horizontal) 650 else ViewGroup.LayoutParams.MATCH_PARENT,
                    if (horizontal) ViewGroup.LayoutParams.MATCH_PARENT else 650,
                )
            }
        }
        rule.runOnIdle { assertThat(if (horizontal) view.width else view.height).isEqualTo(650) }
    }

    @Test
    fun `Fill minimum can exceed the scroll viewport`() {
        lateinit var view: View
        render(if (horizontal) Size(Fill(min = 600u), Fill()) else Size(Fill(), Fill(min = 600u))) { context ->
            FrameLayout(context).apply {
                view = View(context)
                addView(view, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
            }
        }
        rule.runOnIdle {
            assertThat(if (horizontal) view.width else view.height).isEqualTo(with(rule.density) { 600.dp.roundToPx() })
        }
    }

    @Test
    fun `zero capped scroll allocation stays zero through native fallback sizing`() {
        lateinit var view: View
        val size = if (horizontal) Size(Fill(max = 0u), Fill()) else Size(Fill(), Fill(max = 0u))
        rule.setContent {
            val scroll = rememberScrollState()
            val unbounded = remember { mutableStateOf(false) }
            val config = if (horizontal) {
                ConstrainedFillLayout.Config.Horizontal(FlexDistribution.START, Arrangement.Start, Alignment.Top)
            } else {
                ConstrainedFillLayout.Config.Vertical(FlexDistribution.START, Arrangement.Top, Alignment.Start)
            }
            ConstrainedFillLayout(
                config = config,
                spacing = 0.dp,
                modifier = Modifier.requiredSize(200.dp, 400.dp).then(
                    if (horizontal) Modifier.horizontalScroll(scroll) else Modifier.verticalScroll(scroll),
                ),
            ) {
                AndroidView(
                    factory = { context -> FrameLayout(context).also { view = it } },
                    modifier = ComponentSizeParentDataModifier(size)
                        .fillUnboundedAxisFromMinimum(size)
                        .trackMainAxisUnbounded(horizontal, unbounded)
                        .size(webViewEffectiveSize(size, 0, 0, horizontal && unbounded.value, !horizontal && unbounded.value)),
                )
            }
        }
        rule.runOnIdle { assertThat(if (horizontal) view.width else view.height).isZero() }
    }

    @Test
    fun `size overrides preserve the native content instance`() {
        var size by mutableStateOf(Size(Fixed(80u), Fixed(90u)))
        var created = 0
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 400.dp)) {
                val scroll = rememberScrollState()
                BoxWithUnboundedFillMinimums(
                    modifier = Modifier.fillMaxSize().then(
                        if (horizontal) Modifier.horizontalScroll(scroll) else Modifier.verticalScroll(scroll),
                    ),
                    size = size,
                ) { minimums ->
                    AndroidView(
                        factory = { context ->
                            created++
                            View(context)
                        },
                        modifier = minimums.fillUnboundedAxisFromMinimum(size).size(size),
                    )
                }
            }
        }
        rule.runOnIdle {
            assertThat(created).isEqualTo(1)
            size = Size(Fill(), Fill())
        }
        rule.runOnIdle {
            assertThat(created).isEqualTo(1)
            size = Size(Fixed(80u), Fixed(90u))
        }
        rule.runOnIdle { assertThat(created).isEqualTo(1) }
    }

    private fun render(size: Size, factory: (Context) -> View) {
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 400.dp)) {
                val scroll = rememberScrollState()
                AndroidView(
                    factory = factory,
                    modifier = Modifier.fillMaxSize()
                        .then(if (horizontal) Modifier.horizontalScroll(scroll) else Modifier.verticalScroll(scroll))
                        .fillUnboundedAxisFromMinimum(size).size(size),
                )
            }
        }
    }

    companion object {
        @JvmStatic
        @ParameterizedRobolectricTestRunner.Parameters(name = "horizontal={0}")
        fun directions() = listOf(arrayOf(false), arrayOf(true))
    }
}
