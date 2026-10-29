package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fill
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint.Fit
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebViewSizeTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `Fill native child keeps the viewport height under a scroll`() {
        lateinit var view: View
        composeTestRule.setContent {
            Box(Modifier.requiredSize(200.dp, 400.dp)) {
                AndroidView(
                    factory = { context ->
                        FrameLayout(context).apply {
                            view = View(context)
                            addView(
                                view,
                                ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT),
                            )
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .fillUnboundedAxisFromMinimum(Size(Fill(), Fill())),
                )
            }
        }
        composeTestRule.runOnIdle {
            assertThat(view.height).isEqualTo(with(composeTestRule.density) { 400.dp.roundToPx() })
        }
    }

    @Test
    fun `Fit native view can remain taller than the viewport`() {
        lateinit var view: View
        composeTestRule.setContent {
            val context = LocalContext.current
            Box(Modifier.requiredSize(200.dp, 400.dp)) {
                AndroidView(
                    factory = {
                        View(context).also { nativeView ->
                            view = nativeView
                            nativeView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 650)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .fillUnboundedAxisFromMinimum(Size(Fill(), Fit())),
                )
            }
        }
        composeTestRule.runOnIdle { assertThat(view.height).isEqualTo(650) }
    }
}
