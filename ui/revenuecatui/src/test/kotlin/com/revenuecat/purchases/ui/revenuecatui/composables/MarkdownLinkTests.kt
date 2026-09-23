package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.text.LinkAnnotation
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MarkdownLinkTests {

    @get:Rule
    val composeTestRule = createComposeRule()

    /**
     * Screen readers activate a link through its listener rather than a tap, so the listener has to open it through
     * the same UriHandler a tap uses.
     */
    @Test
    fun `link listeners open the link through the UriHandler`(): Unit = with(composeTestRule) {
        val opened = mutableListOf<String>()
        setContent {
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        opened += uri
                    }
                },
            ) {
                Markdown(text = "Read the [terms](https://rev.cat/terms) and **[privacy](https://rev.cat/privacy)**.")
            }
        }

        val text = onNodeWithText("Read the terms and privacy.", useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .single()
        val links = text.getLinkAnnotations(0, text.length).map { it.item as LinkAnnotation.Url }

        assertThat(links).hasSize(2)
        runOnIdle { links.forEach { link -> link.linkInteractionListener?.onClick(link) } }
        assertThat(opened).containsExactly("https://rev.cat/terms", "https://rev.cat/privacy")
    }
}
