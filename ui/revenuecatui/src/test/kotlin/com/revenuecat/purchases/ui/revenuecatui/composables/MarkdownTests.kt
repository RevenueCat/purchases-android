package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTests {

    // MARK: - appendTextWithUnderlines Tests

    @Test
    fun `appendTextWithUnderlines with single underline tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Hello <u>world</u>!", state)
        }

        assertEquals("Hello world!", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertEquals(1, underlineSpans.size)

        val underlinedText = result.text.substring(underlineSpans[0].start, underlineSpans[0].end)
        assertEquals("world", underlinedText)
    }

    @Test
    fun `appendTextWithUnderlines with multiple underline tags`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>one</u> and <u>two</u>", state)
        }

        assertEquals("one and two", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertEquals(2, underlineSpans.size)
    }

    @Test
    fun `appendTextWithUnderlines with no underline tags`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Hello world!", state)
        }

        assertEquals("Hello world!", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isEmpty())
    }

    @Test
    fun `appendTextWithUnderlines with empty underline tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Hello <u></u> world", state)
        }

        assertEquals("Hello  world", result.text)
    }

    @Test
    fun `appendTextWithUnderlines with newline in underline tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>line1\nline2</u>", state)
        }

        assertEquals("line1\nline2", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())
    }

    @Test
    fun `appendTextWithUnderlines with unclosed tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Hello <u>world", state)
        }

        // Unclosed tag should still start underlining from the tag
        assertEquals("Hello world", result.text)

        // The underline style was pushed but never popped
        assertEquals(1, state.underlineDepth)
    }

    @Test
    fun `appendTextWithUnderlines with adjacent underline tags`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>first</u><u>second</u>", state)
        }

        assertEquals("firstsecond", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())
    }

    @Test
    fun `appendTextWithUnderlines with identical underline tags`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>TEST_IDENTICAL</u><u>TEST_IDENTICAL</u>", state)
        }

        assertEquals("TEST_IDENTICALTEST_IDENTICAL", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())
    }

    @Test
    fun `appendTextWithUnderlines with special characters in content`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>\$100 & test</u>", state)
        }

        assertEquals("\$100 & test", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())
    }

    // MARK: - handleInlineHTML Tests

    @Test
    fun `handleInlineHTML with opening underline tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG, state)
            append("underlined text")
        }

        assertEquals(1, state.underlineDepth)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertEquals(1, underlineSpans.size)
    }

    @Test
    fun `handleInlineHTML with closing underline tag`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG, state)
            append("underlined")
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_CLOSE_TAG, state)
            append(" not underlined")
        }

        assertEquals(0, state.underlineDepth)
        assertEquals("underlined not underlined", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertEquals(1, underlineSpans.size)

        val underlinedText = result.text.substring(underlineSpans[0].start, underlineSpans[0].end)
        assertEquals("underlined", underlinedText)
    }

    @Test
    fun `handleInlineHTML with closing tag without opening`() {
        val state = MarkdownState()
        buildAnnotatedString {
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_CLOSE_TAG, state)
        }

        // Should not go negative
        assertEquals(0, state.underlineDepth)
    }

    @Test
    fun `handleInlineHTML with nested underline tags`() {
        val state = MarkdownState()
        buildAnnotatedString {
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG, state)
            handleInlineHTML(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG, state)
        }

        assertEquals(2, state.underlineDepth)
    }

    @Test
    fun `handleInlineHTML ignores unknown tags`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            handleInlineHTML("<b>", state)
            append("text")
        }

        assertEquals(0, state.underlineDepth)
        assertEquals("text", result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    // MARK: - MarkdownState Tests

    @Test
    fun `MarkdownState initializes with zero underline depth`() {
        val state = MarkdownState()
        assertEquals(0, state.underlineDepth)
    }

    @Test
    fun `MarkdownState tracks underline depth correctly`() {
        val state = MarkdownState()

        state.underlineDepth++
        assertEquals(1, state.underlineDepth)

        state.underlineDepth++
        assertEquals(2, state.underlineDepth)

        state.underlineDepth--
        assertEquals(1, state.underlineDepth)

        state.underlineDepth--
        assertEquals(0, state.underlineDepth)
    }

    // MARK: - Combined Formatting Tests

    @Test
    fun `underline combined with manual bold style`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>", state)
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append("bold text")
            pop()
            appendTextWithUnderlines("</u>", state)
        }

        assertEquals("bold text", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())

        val boldSpans = result.spanStyles.filter {
            it.item.fontWeight == FontWeight.Bold
        }
        assertTrue(boldSpans.isNotEmpty())
    }

    @Test
    fun `underline combined with manual italic style`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("<u>", state)
            pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
            append("italic text")
            pop()
            appendTextWithUnderlines("</u>", state)
        }

        assertEquals("italic text", result.text)

        val underlineSpans = result.spanStyles.filter {
            it.item.textDecoration == TextDecoration.Underline
        }
        assertTrue(underlineSpans.isNotEmpty())

        val italicSpans = result.spanStyles.filter {
            it.item.fontStyle == FontStyle.Italic
        }
        assertTrue(italicSpans.isNotEmpty())
    }

    @Test
    fun `plain text returns correct string`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Hello world", state)
        }

        assertEquals("Hello world", result.text)
        assertEquals(0, state.underlineDepth)
    }

    @Test
    fun `preserves newlines in text`() {
        val state = MarkdownState()
        val result = buildAnnotatedString {
            appendTextWithUnderlines("Line 1\nLine 2\n\nLine 4", state)
        }

        assertTrue(result.text.contains("\n"))
        assertEquals("Line 1\nLine 2\n\nLine 4", result.text)
    }
}
