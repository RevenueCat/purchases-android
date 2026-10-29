package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
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

    private fun render(size: Size, factory: (Context) -> View) {
        rule.setContent {
            Box(Modifier.requiredSize(200.dp, 400.dp)) {
                val scroll = rememberScrollState()
                AndroidView(
                    factory = factory,
                    modifier = Modifier.fillMaxSize()
                        .then(if (horizontal) Modifier.horizontalScroll(scroll) else Modifier.verticalScroll(scroll))
                        .fillUnboundedAxisFromMinimum(size),
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
