package com.revenuecat.purchases.ui.revenuecatui.composables

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class MarkdownPlainTextTests {

    @Test
    fun `drops formatting and link targets`() {
        assertThat(markdownPlainText("**Bold** and [terms](https://rev.cat/terms)")).isEqualTo("Bold and terms")
    }

    @Test
    fun `keeps every displayed block, one per line`() {
        val markdown = "# Title\n\nParagraph\n\n- Item\n\n> Quote\n\n```\nCode\n```"

        assertThat(markdownPlainText(markdown)).isEqualTo("Title\nParagraph\nItem\nQuote\nCode")
    }
}
