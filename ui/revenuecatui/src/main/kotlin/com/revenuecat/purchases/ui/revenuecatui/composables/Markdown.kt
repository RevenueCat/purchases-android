@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.revenuecat.purchases.ui.revenuecatui.extensions.conditional
import org.commonmark.ext.gfm.strikethrough.Strikethrough
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension
import org.commonmark.node.BlockQuote
import org.commonmark.node.BulletList
import org.commonmark.node.Code
import org.commonmark.node.Document
import org.commonmark.node.Emphasis
import org.commonmark.node.FencedCodeBlock
import org.commonmark.node.HardLineBreak
import org.commonmark.node.Heading
import org.commonmark.node.HtmlInline
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.SoftLineBreak
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

// Inspired by https://github.com/ErikHellman/MarkdownComposer/blob/master/app/src/main/java/se/hellsoft/markdowncomposer/MarkdownComposer.kt

private val parser = Parser.builder()
    .extensions(listOf(StrikethroughExtension.create()))
    .build()

/**
 * Tracks state during markdown AST traversal, specifically for handling
 * underline tags that span across multiple AST nodes.
 */
internal class MarkdownState {
    var underlineDepth = 0
}

internal object MarkdownTagDefinitions {
    const val UNDERLINE_OPEN_TAG = "<u>"
    const val UNDERLINE_CLOSE_TAG = "</u>"
}

/**
 * @param allowLinks If true, links will be decorated and clickable.
 * @param textFillMaxWidth If true, the text will fill the maximum width available. This was used by paywalls V1 and
 * left to avoid unintended UI changes.
 * @param applyFontSizeToParagraph If true, the provided [fontSize] will be applied to the annotated string used to
 * build a Markdown paragraph from the [text]. This was not the case in Paywalls V1, but is needed for Paywalls V2.
 * (See `TextComponentViewTests` for more info.)
 */
@SuppressWarnings("LongParameterList")
@Composable
internal fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontSize: TextUnit = TextUnit.Unspecified,
    fontWeight: FontWeight? = null,
    fontFamily: FontFamily? = null,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    textAlign: TextAlign? = null,
    allowLinks: Boolean = true,
    // The below parameters are used to avoid unintended changes to V1 paywalls.
    textFillMaxWidth: Boolean = false,
    applyFontSizeToParagraph: Boolean = true,
) {
    val root = parser.parse(text) as Document

    val density = LocalDensity.current
    val paragraphPadding = with(density) {
        if (style.lineHeight.type == TextUnitType.Sp) {
            style.lineHeight.toDp()
        } else {
            0.dp
        }
    }

    Column(
        verticalArrangement = Arrangement.spacedBy(paragraphPadding),
        horizontalAlignment = horizontalAlignment,
        modifier = modifier,
    ) {
        MDDocument(
            root,
            color,
            style,
            fontSize,
            fontWeight,
            fontFamily,
            textAlign,
            allowLinks,
            textFillMaxWidth,
            applyFontSizeToParagraph,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDDocument(
    document: Document,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
    applyFontSizeToParagraph: Boolean,
) {
    MDBlockChildren(
        document,
        color,
        style,
        fontSize,
        fontWeight,
        fontFamily,
        textAlign,
        allowLinks,
        textFillMaxWidth,
        applyFontSizeToParagraph,
    )
}

@SuppressWarnings("LongParameterList", "MagicNumber")
@Composable
private fun MDHeading(
    heading: Heading,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
    applyFontSizeToParagraph: Boolean,
    modifier: Modifier = Modifier,
) {
    val overriddenStyle = when (heading.level) {
        1 -> MaterialTheme.typography.headlineLarge
        2 -> MaterialTheme.typography.headlineMedium
        3 -> MaterialTheme.typography.headlineSmall
        4 -> MaterialTheme.typography.titleLarge
        5 -> MaterialTheme.typography.titleMedium
        6 -> MaterialTheme.typography.titleSmall
        else -> {
            // Invalid header...
            MDBlockChildren(
                heading,
                color,
                style,
                fontSize,
                fontWeight,
                fontFamily,
                textAlign,
                allowLinks,
                textFillMaxWidth,
                applyFontSizeToParagraph,
            )
            return
        }
    }

    val padding = if (heading.parent is Document) 8.dp else 0.dp
    Box(modifier = modifier.padding(bottom = padding)) {
        val text = buildAnnotatedString {
            appendMarkdownChildren(heading, color, allowLinks, baseFontWeight = fontWeight)
        }
        MarkdownText(
            text,
            color,
            overriddenStyle,
            TextUnit.Unspecified,
            fontWeight,
            fontFamily,
            textAlign,
            textFillMaxWidth,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDParagraph(
    paragraph: Paragraph,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
    applyFontSizeToParagraph: Boolean,
) {
    Box {
        val styledText = buildAnnotatedString {
            pushStyle(
                style
                    .copy(
                        color = color,
                        fontWeight = fontWeight,
                        fontSize = if (applyFontSizeToParagraph) fontSize else style.fontSize,
                        fontFamily = fontFamily,
                    )
                    .toSpanStyle(),
            )
            appendMarkdownChildren(paragraph as Node, color, allowLinks, baseFontWeight = fontWeight)
            pop()
        }
        MarkdownText(
            styledText,
            color,
            style,
            fontSize,
            fontWeight,
            fontFamily,
            textAlign,
            textFillMaxWidth,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDBulletList(
    bulletList: BulletList,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
) {
    val marker = bulletList.bulletMarker
    MDListItems(
        bulletList,
        color = color,
        style = style,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        allowLinks = allowLinks,
        textFillMaxWidth = textFillMaxWidth,
    ) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.typography.bodyLarge.toSpanStyle())
            append("$marker ")
            appendMarkdownChildren(it, color, allowLinks, baseFontWeight = fontWeight)
            pop()
        }
        MarkdownText(
            text,
            color,
            style,
            fontSize,
            fontWeight,
            fontFamily,
            textAlign,
            textFillMaxWidth,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDOrderedList(
    orderedList: OrderedList,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
) {
    var number = orderedList.startNumber
    val delimiter = orderedList.delimiter
    MDListItems(
        orderedList,
        color = color,
        style = style,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        allowLinks = allowLinks,
        textFillMaxWidth = textFillMaxWidth,
    ) {
        val text = buildAnnotatedString {
            pushStyle(style.toSpanStyle())
            append("${number++}$delimiter ")
            appendMarkdownChildren(it, color, allowLinks, baseFontWeight = fontWeight)
            pop()
        }
        MarkdownText(
            text,
            color,
            style,
            fontSize,
            fontWeight,
            fontFamily,
            textAlign,
            textFillMaxWidth,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDListItems(
    listBlock: ListBlock,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
    modifier: Modifier = Modifier,
    item: @Composable (node: Node) -> Unit,
) {
    val bottom = if (listBlock.parent is Document) 8.dp else 0.dp
    val start = if (listBlock.parent is Document) 0.dp else 8.dp
    Column(modifier = modifier.padding(start = start, bottom = bottom)) {
        var listItem = listBlock.firstChild
        while (listItem != null) {
            var child = listItem.firstChild
            while (child != null) {
                when (child) {
                    is BulletList ->
                        MDBulletList(
                            child,
                            color,
                            style,
                            fontSize,
                            fontWeight,
                            fontFamily,
                            textAlign,
                            allowLinks,
                            textFillMaxWidth,
                        )
                    is OrderedList ->
                        MDOrderedList(
                            child,
                            color,
                            style,
                            fontSize,
                            fontWeight,
                            fontFamily,
                            textAlign,
                            allowLinks,
                            textFillMaxWidth,
                        )
                    else -> item(child)
                }
                child = child.next
            }
            listItem = listItem.next
        }
    }
}

@Composable
private fun MDBlockQuote(
    blockQuote: BlockQuote,
    color: Color,
    allowLinks: Boolean,
    baseFontWeight: FontWeight?,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .drawBehind {
                drawLine(
                    color = color,
                    strokeWidth = 2f,
                    start = Offset(12.dp.value, 0f),
                    end = Offset(12.dp.value, size.height),
                )
            }
            .padding(start = 16.dp, top = 4.dp, bottom = 4.dp),
    ) {
        val text = buildAnnotatedString {
            pushStyle(
                MaterialTheme.typography.bodyLarge.toSpanStyle()
                    .plus(SpanStyle(fontStyle = FontStyle.Italic)),
            )
            appendMarkdownChildren(blockQuote, color, allowLinks, baseFontWeight)
            pop()
        }
        Text(text, modifier)
    }
}

@Composable
private fun MDFencedCodeBlock(fencedCodeBlock: FencedCodeBlock, modifier: Modifier = Modifier) {
    val padding = if (fencedCodeBlock.parent is Document) 8.dp else 0.dp
    Box(modifier = modifier.padding(start = 8.dp, bottom = padding)) {
        Text(
            text = fencedCodeBlock.literal,
            style = TextStyle(fontFamily = FontFamily.Monospace),
            modifier = modifier,
        )
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDBlockChildren(
    parent: Node,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    allowLinks: Boolean,
    textFillMaxWidth: Boolean,
    applyFontSizeToParagraph: Boolean,
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is BlockQuote -> MDBlockQuote(child, color, allowLinks, baseFontWeight = fontWeight)
            is Heading -> MDHeading(
                child,
                color,
                style,
                fontSize,
                fontWeight,
                fontFamily,
                textAlign,
                allowLinks,
                textFillMaxWidth,
                applyFontSizeToParagraph,
            )
            is Paragraph -> MDParagraph(
                child,
                color,
                style,
                fontSize,
                fontWeight,
                fontFamily,
                textAlign,
                allowLinks,
                textFillMaxWidth,
                applyFontSizeToParagraph,
            )
            is FencedCodeBlock -> MDFencedCodeBlock(child)
            is BulletList -> MDBulletList(
                child,
                color,
                style,
                fontSize,
                fontWeight,
                fontFamily,
                textAlign,
                allowLinks,
                textFillMaxWidth,
            )
            is OrderedList -> MDOrderedList(
                child,
                color,
                style,
                fontSize,
                fontWeight,
                fontFamily,
                textAlign,
                allowLinks,
                textFillMaxWidth,
            )
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendMarkdownChildren(
    parent: Node,
    color: Color,
    allowLinks: Boolean,
    baseFontWeight: FontWeight?,
    state: MarkdownState = MarkdownState(),
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is Paragraph -> appendMarkdownChildren(child, color, allowLinks, baseFontWeight, state)
            is Text -> appendTextWithUnderlines(child.literal, state)
            is Emphasis -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendMarkdownChildren(child, color, allowLinks, baseFontWeight, state)
                pop()
            }
            is StrongEmphasis -> {
                val biggerWeight = if ((baseFontWeight?.weight ?: 0) > FontWeight.Bold.weight) {
                    baseFontWeight
                } else {
                    FontWeight.Bold
                }
                pushStyle(SpanStyle(fontWeight = biggerWeight))
                appendMarkdownChildren(child, color, allowLinks, biggerWeight, state)
                pop()
            }
            is Code -> {
                pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                appendTextWithUnderlines(child.literal, state)
                pop()
            }
            is HardLineBreak, is SoftLineBreak -> {
                appendLine()
            }
            is Link -> {
                if (allowLinks) {
                    val underline = SpanStyle(color, textDecoration = TextDecoration.Underline)
                    withLink(LinkAnnotation.Url(child.destination, TextLinkStyles(underline))) {
                        appendMarkdownChildren(child, color, allowLinks = true, baseFontWeight = baseFontWeight, state)
                    }
                } else {
                    appendMarkdownChildren(child, color, allowLinks = false, baseFontWeight = baseFontWeight, state)
                }
            }
            is Strikethrough -> {
                pushStyle(TextStyle(textDecoration = TextDecoration.LineThrough).toSpanStyle())
                appendMarkdownChildren(child, color, allowLinks, baseFontWeight, state)
                pop()
            }
            is HtmlInline -> handleInlineHTML(child.literal, state)
        }
        child = child.next
    }
}

internal fun AnnotatedString.Builder.handleInlineHTML(tag: String, state: MarkdownState) {
    // Handle <u> and </u> tags for underline support
    when (tag) {
        MarkdownTagDefinitions.UNDERLINE_OPEN_TAG -> {
            pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
            state.underlineDepth++
        }
        MarkdownTagDefinitions.UNDERLINE_CLOSE_TAG -> {
            if (state.underlineDepth > 0) {
                pop()
                state.underlineDepth--
            }
        }
    }
}

/**
 * Processes text content, handling `<u>...</u>` underline tags.
 * Pushes/pops underline style when encountering opening/closing tags,
 * allowing underlines to span across multiple AST nodes (e.g., `<u>**bold**</u>`).
 */
internal fun AnnotatedString.Builder.appendTextWithUnderlines(
    text: String,
    state: MarkdownState,
) {
    var remaining = text
    while (remaining.isNotEmpty()) {
        val openIdx = remaining.indexOf(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG)
        val closeIdx = remaining.indexOf(MarkdownTagDefinitions.UNDERLINE_CLOSE_TAG)

        when {
            openIdx == 0 -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.Underline))
                state.underlineDepth++
                remaining = remaining.substring(MarkdownTagDefinitions.UNDERLINE_OPEN_TAG.length)
            }
            closeIdx == 0 -> {
                if (state.underlineDepth > 0) {
                    pop()
                    state.underlineDepth--
                }
                remaining = remaining.substring(MarkdownTagDefinitions.UNDERLINE_CLOSE_TAG.length)
            }
            openIdx > 0 && (closeIdx < 0 || openIdx < closeIdx) -> {
                append(remaining.substring(0, openIdx))
                remaining = remaining.substring(openIdx)
            }
            closeIdx > 0 && (openIdx < 0 || closeIdx < openIdx) -> {
                append(remaining.substring(0, closeIdx))
                remaining = remaining.substring(closeIdx)
            }
            else -> {
                append(remaining)
                remaining = ""
            }
        }
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MarkdownText(
    text: AnnotatedString,
    color: Color,
    style: TextStyle,
    fontSize: TextUnit,
    fontWeight: FontWeight?,
    fontFamily: FontFamily?,
    textAlign: TextAlign?,
    textFillMaxWidth: Boolean,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        color = color,
        style = style,
        fontSize = fontSize,
        fontWeight = fontWeight,
        fontFamily = fontFamily,
        textAlign = textAlign,
        modifier = modifier
            .conditional(textFillMaxWidth) {
                fillMaxWidth()
            },
    )
}

@Preview
@Composable
@Suppress("MaxLineLength")
private fun PreviewText() {
    Surface {
        Markdown(
            text = "Hello, world\n**bold**\n_italic_ \n`code`\n<u>underline</u>\n<u>**_underlined italic bold_**</u>\n[RevenueCat](https://revenuecat.com)",
            modifier = Modifier.padding(20.dp),
        )
    }
}
