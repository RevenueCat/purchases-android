package com.revenuecat.purchases.ui.revenuecatui.composables

import android.text.Spanned
import android.text.style.ClickableSpan
import android.text.style.URLSpan
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeProvider
import androidx.activity.ComponentActivity
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.node.RootForTest
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalComposeUiApi::class)
internal class MarkdownLinkAccessibilityInstrumentedTests {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    /**
     * TalkBack opens a link through the span it finds in the node's text, not through a tap. A URLSpan opens the URL
     * itself, skipping the paywall's UriHandler, which tracks the interaction and notifies the app's listener.
     * A plain ClickableSpan calls back into the link's listener instead.
     */
    @Test
    fun linksExposedToScreenReadersCallBackIntoThePaywall(): Unit = with(composeTestRule) {
        setContent {
            Markdown(text = "Read the [terms](https://rev.cat/terms) and [privacy](https://rev.cat/privacy).")
        }
        val node = onNodeWithText("Read the terms and privacy.", useUnmergedTree = true).fetchSemanticsNode()
        val root: RootForTest = requireNotNull(node.root)

        val linkSpans = runOnIdle {
            root.forceAccessibilityForTesting(true)
            val text = activity.window.decorView.findAccessibilityNodeProvider()
                .createAccessibilityNodeInfo(node.id)
                ?.text as Spanned
            root.forceAccessibilityForTesting(false)
            text.getSpans(0, text.length, ClickableSpan::class.java).toList()
        }

        assertThat(linkSpans).hasSize(2)
        assertThat(linkSpans).noneMatch { it is URLSpan }
    }

    private fun View.findAccessibilityNodeProvider(): AccessibilityNodeProvider =
        findAccessibilityNodeProviderOrNull() ?: error("Expected the Compose view to provide accessibility nodes")

    private fun View.findAccessibilityNodeProviderOrNull(): AccessibilityNodeProvider? =
        accessibilityNodeProvider
            ?: (this as? ViewGroup)?.let { group ->
                (0 until group.childCount).firstNotNullOfOrNull { index ->
                    group.getChildAt(index).findAccessibilityNodeProviderOrNull()
                }
            }
}
