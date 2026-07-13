package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.RenderProcessGoneDetail
import android.webkit.WebView
import androidx.compose.foundation.layout.Box
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
internal class WebViewFailurePathTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `renderer termination shows fallback and destroys the web view`() {
        var destroyed = false
        var identity by mutableStateOf("a")
        var showFallback = false

        composeTestRule.setContent {
            key(identity) {
                var loadFailed by remember { mutableStateOf(false) }
                showFallback = loadFailed
                if (loadFailed) {
                    Text("fallback")
                } else {
                    val context = LocalContext.current
                    val holder = remember { WebViewBridgeHolder() }
                    AndroidView(
                        factory = {
                            WebView(context).apply {
                                val client = PaywallWebViewClient(
                                    expectedOrigin = "https://assets.example.com",
                                    onMainFrameNavigationStarted = {},
                                    onMainFrameLoadFailed = { loadFailed = true },
                                )
                                setWebViewClient(client)
                                // Simulate renderer death immediately after creation.
                                client.onRenderProcessGone(this, mockk<RenderProcessGoneDetail>(relaxed = true))
                            }
                        },
                        onRelease = { webView ->
                            holder.bridge?.release()
                            holder.bridge = null
                            webView.destroy()
                            destroyed = true
                        },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("fallback").assertIsDisplayed()
        assertThat(destroyed).isTrue()
        assertThat(showFallback).isTrue()
    }

    @Test
    fun `without fallback the dead android view is removed`() {
        var webViewPresent = true
        composeTestRule.setContent {
            var loadFailed by remember { mutableStateOf(false) }
            if (loadFailed) {
                webViewPresent = false
                Box {}
            } else {
                val context = LocalContext.current
                AndroidView(
                    factory = {
                        WebView(context).apply {
                            val client = PaywallWebViewClient(
                                expectedOrigin = "https://assets.example.com",
                                onMainFrameNavigationStarted = {},
                                onMainFrameLoadFailed = { loadFailed = true },
                            )
                            setWebViewClient(client)
                            client.onRenderProcessGone(this, mockk(relaxed = true))
                        }
                    },
                    onRelease = { it.destroy() },
                )
            }
        }
        composeTestRule.waitForIdle()
        assertThat(webViewPresent).isFalse()
    }

    @Test
    fun `identity change after failure can create a replacement web view`() {
        val creations = mutableListOf<String>()
        var identity by mutableStateOf("one")
        var failNext = true

        composeTestRule.setContent {
            key(identity) {
                var loadFailed by remember { mutableStateOf(false) }
                if (loadFailed) {
                    Text("failed-$identity")
                } else {
                    val context = LocalContext.current
                    AndroidView(
                        factory = {
                            creations.add(identity)
                            WebView(context).apply {
                                if (failNext) {
                                    failNext = false
                                    val client = PaywallWebViewClient(
                                        expectedOrigin = "https://assets.example.com",
                                        onMainFrameNavigationStarted = {},
                                        onMainFrameLoadFailed = { loadFailed = true },
                                    )
                                    setWebViewClient(client)
                                    client.onRenderProcessGone(this, mockk(relaxed = true))
                                }
                            }
                        },
                        onRelease = { it.destroy() },
                    )
                }
            }
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("failed-one").assertIsDisplayed()

        identity = "two"
        composeTestRule.waitForIdle()

        assertThat(creations).containsExactly("one", "two")
    }
}
