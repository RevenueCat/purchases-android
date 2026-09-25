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

    @Test
    fun `finds links`() {
        assertThat(markdownHasLinks("See [terms](https://rev.cat/terms)")).isTrue()
        assertThat(markdownHasLinks("- See [plans](myapp:plans/month)")).isTrue()
        assertThat(markdownHasLinks("See <https://rev.cat/terms>")).isTrue()
    }

    @Test
    fun `finds no links in plain copy`() {
        assertThat(markdownHasLinks("$5.83/mo, billed yearly")).isFalse()
        assertThat(markdownHasLinks("**[Best value]** at 5 < 6")).isFalse()
    }
}
