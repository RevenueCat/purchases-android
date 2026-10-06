package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withLink
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A stand-in stands in for a step the SDK has not resolved yet, so nothing under it may act on a
 * touch. The placeholder only greys content out, so a button or a markdown link stays live
 * underneath unless the container swallows the event before its children see it.
 */
@RunWith(AndroidJUnit4::class)
internal class WorkflowSkeletonInputTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setUpChild(blocked: Boolean, onClick: () -> Unit) {
        composeTestRule.setContent {
            Box(Modifier.fillMaxSize().let { if (blocked) it.blockInput() else it }) {
                Box(Modifier.fillMaxSize().clickable { onClick() })
            }
        }
    }

    private fun setUpLink(blocked: Boolean, handler: UriHandler) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalUriHandler provides handler) {
                Box(Modifier.fillMaxSize().let { if (blocked) it.blockInput() else it }) {
                    BasicText(
                        buildAnnotatedString {
                            withLink(LinkAnnotation.Url("https://example.com")) { append("terms") }
                        },
                    )
                }
            }
        }
    }

    private fun countingHandler(count: () -> Unit) = object : UriHandler {
        override fun openUri(uri: String) = count()
    }

    @Test
    fun `a tap reaches a button when the step is not a stand-in`() {
        var clicks = 0
        setUpChild(blocked = false) { clicks++ }

        composeTestRule.onRoot().performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertThat(clicks).isOne()
    }

    @Test
    fun `a stand-in swallows a tap that would otherwise reach a button`() {
        var clicks = 0
        setUpChild(blocked = true) { clicks++ }

        composeTestRule.onRoot().performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertThat(clicks).isZero()
    }

    @Test
    fun `a tap opens a link when the step is not a stand-in`() {
        var opened = 0
        setUpLink(blocked = false, handler = countingHandler { opened++ })

        composeTestRule.onNodeWithText("terms").performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertThat(opened).isOne()
    }

    @Test
    fun `a stand-in swallows a tap on a markdown link`() {
        var opened = 0
        setUpLink(blocked = true, handler = countingHandler { opened++ })

        composeTestRule.onNodeWithText("terms").performTouchInput { click() }
        composeTestRule.waitForIdle()

        assertThat(opened).isZero()
    }
}
