package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.graphics.Color
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

        val links = linkAnnotations("Read the terms and privacy.")

        assertThat(links).hasSize(2)
        runOnIdle { links.forEach { link -> link.linkInteractionListener?.onClick(link) } }
        assertThat(opened).containsExactly("https://rev.cat/terms", "https://rev.cat/privacy")
    }

    /**
     * The platform handler throws when no app can open a link. Compose swallows that for links without a listener,
     * so the listener has to as well.
     */
    @Test
    fun `link listeners ignore links no app can open`(): Unit = with(composeTestRule) {
        setContent {
            CompositionLocalProvider(
                LocalUriHandler provides object : UriHandler {
                    override fun openUri(uri: String) {
                        throw IllegalArgumentException("Can't open $uri")
                    }
                },
            ) {
                Markdown(text = "Read the [terms](https://rev.cat/terms).")
            }
        }

        val link = linkAnnotations("Read the terms.").single()

        runOnIdle { link.linkInteractionListener?.onClick(link) }
    }

    /**
     * An annotation that compares equal across rebuilds lets Compose skip laying the text out again.
     */
    @Test
    fun `link annotations are equal across rebuilds`() {
        val handler = object : UriHandler {
            override fun openUri(uri: String) = Unit
        }
        fun build() = linkAnnotation("https://rev.cat/terms", Color.Black, handler)

        assertThat(build()).isEqualTo(build())
    }

    private fun ComposeContentTestRule.linkAnnotations(text: String): List<LinkAnnotation.Url> {
        val annotated = onNodeWithText(text, useUnmergedTree = true)
            .fetchSemanticsNode()
            .config
            .getOrNull(SemanticsProperties.Text)
            .orEmpty()
            .single()
        return annotated.getLinkAnnotations(0, annotated.length).map { it.item as LinkAnnotation.Url }
    }
}
