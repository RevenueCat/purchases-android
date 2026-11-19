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
    fun `keeps thematic break lines as typed`() {
        assertThat(markdownPlainText("---")).isEqualTo("---")
        assertThat(markdownPlainText("- - - - -")).isEqualTo("- - - - -")
        assertThat(markdownPlainText("Before\n\n***\n\nAfter")).isEqualTo("Before\n***\nAfter")
    }

    @Test
    fun `keeps thematic break lines inside quotes, after tabs and with any line ending`() {
        assertThat(markdownPlainText("> ---")).isEqualTo("---")
        assertThat(markdownPlainText("> Quote\n>\n> ***")).isEqualTo("Quote\n***")
        assertThat(markdownPlainText("Before\r\n\r\n- - -\r\n\r\nAfter")).isEqualTo("Before\n- - -\nAfter")
        assertThat(markdownPlainText("Before\r\rAfter\r\r___")).isEqualTo("Before\nAfter\n___")
        assertThat(markdownPlainText("1.\t***")).isEqualTo("***")
    }

    @Test
    fun `keeps a dash or equals line under a text as typed`() {
        assertThat(markdownPlainText("Price\n---")).isEqualTo("Price\n---")
        assertThat(markdownPlainText("Price\n===")).isEqualTo("Price\n===")
        assertThat(markdownPlainText("**Price**\nper week\n---")).isEqualTo("Price\nper week\n---")
        assertThat(markdownPlainText("> Price\n> ---")).isEqualTo("Price\n---")
    }

    @Test
    fun `keeps indented and html texts as typed`() {
        assertThat(markdownPlainText("    Indented")).isEqualTo("    Indented")
        assertThat(markdownPlainText("\tIndented")).isEqualTo("\tIndented")
        assertThat(markdownPlainText("Intro\n\n    - one\n\n    - two")).isEqualTo("Intro\n    - one\n\n    - two")
        assertThat(markdownPlainText("<div>\nSale\n</div>")).isEqualTo("<div>\nSale\n</div>")
        assertThat(markdownPlainText("> <div>\n> Sale")).isEqualTo("<div>\nSale")
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
