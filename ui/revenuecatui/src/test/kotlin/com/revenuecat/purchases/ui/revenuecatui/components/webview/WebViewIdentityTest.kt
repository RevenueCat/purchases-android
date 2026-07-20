package com.revenuecat.purchases.ui.revenuecatui.components.webview

import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.viewinterop.AndroidView
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import org.junit.runners.model.Statement
import org.junit.runner.Description

@RunWith(AndroidJUnit4::class)
internal class WebViewIdentityTest {

    val composeTestRule = createComposeRule()

    /**
     * Mocks must outlive [composeTestRule] teardown: AndroidView.onRelease (and thus
     * [WebViewJavaScriptBridge.release]) runs when the composition is disposed, which is after
     * `@After` methods.
     */
    @get:Rule
    val rules: TestRule = RuleChain
        .outerRule(
            object : TestRule {
                override fun apply(base: Statement, description: Description): Statement {
                    return object : Statement() {
                        override fun evaluate() {
                            mockkStatic(WebViewFeature::class, WebViewCompat::class)
                            every {
                                WebViewFeature.isFeatureSupported(WebViewFeature.WEB_MESSAGE_LISTENER)
                            } returns true
                            every {
                                WebViewCompat.addWebMessageListener(any(), any(), any(), any())
                            } just Runs
                            every {
                                WebViewCompat.removeWebMessageListener(any(), any())
                            } just Runs
                            try {
                                base.evaluate()
                            } finally {
                                unmockkStatic(WebViewFeature::class, WebViewCompat::class)
                            }
                        }
                    }
                }
            },
        )
        .around(composeTestRule)

    @Test
    fun `identity differs when component id changes`() {
        val base = WebViewIdentity(
            resolvedUrl = "https://assets.example.com/a.html",
            componentId = "one",
            sizeToContentWidth = false,
            sizeToContentHeight = true,
        )
        val changed = base.copy(componentId = "two")

        assertThat(base).isNotEqualTo(changed)
    }

    @Test
    fun `identity differs when fit width or height changes`() {
        val base = WebViewIdentity(
            resolvedUrl = "https://assets.example.com/a.html",
            componentId = "one",
            sizeToContentWidth = false,
            sizeToContentHeight = false,
        )

        assertThat(base).isNotEqualTo(base.copy(sizeToContentWidth = true))
        assertThat(base).isNotEqualTo(base.copy(sizeToContentHeight = true))
    }

    @Test
    fun `same url with different component id recreates the web view and bridge`() {
        val creations = mutableListOf<String?>()
        val releases = mutableListOf<String?>()
        var identity by mutableStateOf(
            WebViewIdentity(
                resolvedUrl = "https://assets.example.com/a.html",
                componentId = "one",
                sizeToContentWidth = false,
                sizeToContentHeight = false,
            ),
        )

        composeTestRule.setContent {
            TestWebViewSlot(
                identity = identity,
                onCreated = { creations.add(it) },
                onReleased = { releases.add(it) },
            )
        }
        composeTestRule.waitForIdle()
        assertThat(creations).containsExactly("one")

        identity = identity.copy(componentId = "two")
        composeTestRule.waitForIdle()

        assertThat(releases).containsExactly("one")
        assertThat(creations).containsExactly("one", "two")
    }

    @Test
    fun `fixed-to-fit width and height changes recreate the web view`() {
        val creations = mutableListOf<WebViewIdentity>()
        var identity by mutableStateOf(
            WebViewIdentity(
                resolvedUrl = "https://assets.example.com/a.html",
                componentId = "one",
                sizeToContentWidth = false,
                sizeToContentHeight = false,
            ),
        )

        composeTestRule.setContent {
            TestWebViewSlot(
                identity = identity,
                onCreated = { creations.add(identity) },
                onReleased = {},
            )
        }
        composeTestRule.waitForIdle()

        identity = identity.copy(sizeToContentWidth = true)
        composeTestRule.waitForIdle()
        identity = identity.copy(sizeToContentHeight = true)
        composeTestRule.waitForIdle()

        assertThat(creations).hasSize(3)
        assertThat(creations[1].sizeToContentWidth).isTrue()
        assertThat(creations[2].sizeToContentHeight).isTrue()
    }

    @Test
    fun `old onRelease cannot release the replacement bridge`() {
        val releasedBridges = mutableListOf<WebViewJavaScriptBridge>()
        var identity by mutableStateOf(
            WebViewIdentity(
                resolvedUrl = "https://assets.example.com/a.html",
                componentId = "one",
                sizeToContentWidth = false,
                sizeToContentHeight = false,
            ),
        )
        val liveBridges = mutableListOf<WebViewJavaScriptBridge>()

        composeTestRule.setContent {
            TestWebViewSlot(
                identity = identity,
                onCreated = {},
                onReleased = {},
                onBridgeCreated = { liveBridges.add(it) },
                onBridgeReleased = { releasedBridges.add(it) },
            )
        }
        composeTestRule.waitForIdle()
        val first = liveBridges.single()

        identity = identity.copy(componentId = "two")
        composeTestRule.waitForIdle()

        assertThat(releasedBridges).containsExactly(first)
        assertThat(liveBridges).hasSize(2)
        assertThat(releasedBridges).doesNotContain(liveBridges.last())
    }

    @Test
    fun `measured dimensions start at zero for a new identity`() {
        val initialSizes = mutableListOf<Pair<Int, Int>>()
        var identity by mutableStateOf(
            WebViewIdentity(
                resolvedUrl = "https://assets.example.com/a.html",
                componentId = "one",
                sizeToContentWidth = true,
                sizeToContentHeight = true,
            ),
        )

        composeTestRule.setContent {
            key(identity) {
                var contentWidthCssPx by remember { mutableIntStateOf(0) }
                var contentHeightCssPx by remember { mutableIntStateOf(0) }
                DisposableEffect(Unit) {
                    initialSizes.add(contentWidthCssPx to contentHeightCssPx)
                    contentWidthCssPx = 100
                    onDispose { }
                }
            }
        }
        composeTestRule.waitForIdle()
        assertThat(initialSizes).containsExactly(0 to 0)

        identity = identity.copy(componentId = "two")
        composeTestRule.waitForIdle()
        assertThat(initialSizes).containsExactly(0 to 0, 0 to 0)
    }
}

/**
 * Mirrors the identity-scoped AndroidView / bridge-holder pattern used by [WebViewComponentView]
 * so lifecycle guarantees can be asserted without spinning up a full paywall state tree.
 */
@Composable
private fun TestWebViewSlot(
    identity: WebViewIdentity,
    onCreated: (String?) -> Unit,
    onReleased: (String?) -> Unit,
    onBridgeCreated: (WebViewJavaScriptBridge) -> Unit = {},
    onBridgeReleased: (WebViewJavaScriptBridge) -> Unit = {},
) {
    key(identity) {
        val bridgeHolder = remember { WebViewBridgeHolder() }
        val context = LocalContext.current
        AndroidView(
            factory = {
                WebView(context).apply {
                    val bridge = identity.componentId?.let { id ->
                        WebViewJavaScriptBridge(
                            webView = this,
                            componentId = id,
                            expectedUrl = identity.resolvedUrl,
                            sizeToContentWidth = identity.sizeToContentWidth,
                            sizeToContentHeight = identity.sizeToContentHeight,
                        ).also { created ->
                            created.attach()
                            onBridgeCreated(created)
                        }
                    }
                    bridgeHolder.bridge = bridge
                    onCreated(identity.componentId)
                }
            },
            onRelease = { webView ->
                val bridge = bridgeHolder.bridge
                bridgeHolder.bridge = null
                if (bridge != null) {
                    onBridgeReleased(bridge)
                    bridge.release()
                }
                onReleased(identity.componentId)
                webView.destroy()
            },
        )
    }
}
