@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
import org.commonmark.node.Link
import org.commonmark.node.ListBlock
import org.commonmark.node.Node
import org.commonmark.node.OrderedList
import org.commonmark.node.Paragraph
import org.commonmark.node.StrongEmphasis
import org.commonmark.node.Text
import org.commonmark.parser.Parser

// Inspired by https://github.com/ErikHellman/MarkdownComposer/blob/master/app/src/main/java/se/hellsoft/markdowncomposer/MarkdownComposer.kt

private const val TAG_URL = "url"
private val parser = Parser.builder()
    .extensions(listOf(StrikethroughExtension.create()))
    .build()

@SuppressWarnings("LongParameterList")
@Composable
internal fun Markdown(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
    style: TextStyle = TextStyle.Default,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
) {
    val root = parser.parse(text) as Document

    Column {
        MDDocument(root, color, style, fontWeight, textAlign, modifier)
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDDocument(
    document: Document,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier,
) {
    MDBlockChildren(document, color, style, fontWeight, textAlign, modifier)
}

@SuppressWarnings("LongParameterList", "MagicNumber")
@Composable
private fun MDHeading(
    heading: Heading,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
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
            MDBlockChildren(heading, color, style, fontWeight, textAlign, modifier)
            return
        }
    }

    val padding = if (heading.parent is Document) 8.dp else 0.dp
    Box(modifier = modifier.padding(bottom = padding)) {
        val text = buildAnnotatedString {
            appendMarkdownChildren(heading, color)
        }
        MarkdownText(text, color, overriddenStyle, fontWeight, textAlign, modifier)
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDParagraph(
    paragraph: Paragraph,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        val styledText = buildAnnotatedString {
            pushStyle(
                style
                    .copy(fontWeight = fontWeight)
                    .toSpanStyle(),
            )
            appendMarkdownChildren(paragraph as Node, color)
            pop()
        }
        MarkdownText(styledText, color, style, fontWeight, textAlign, modifier)
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDBulletList(
    bulletList: BulletList,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier = Modifier,
) {
    val marker = bulletList.bulletMarker
    MDListItems(
        bulletList,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        modifier = modifier,
    ) {
        val text = buildAnnotatedString {
            pushStyle(MaterialTheme.typography.bodyLarge.toSpanStyle())
            append("$marker ")
            appendMarkdownChildren(it, color)
            pop()
        }
        MarkdownText(text, color, style, fontWeight, textAlign, modifier)
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDOrderedList(
    orderedList: OrderedList,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier = Modifier,
) {
    var number = orderedList.startNumber
    val delimiter = orderedList.delimiter
    MDListItems(
        orderedList,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        modifier = modifier,
    ) {
        val text = buildAnnotatedString {
            pushStyle(style.toSpanStyle())
            append("${number++}$delimiter ")
            appendMarkdownChildren(it, color)
            pop()
        }
        MarkdownText(text, color, style, fontWeight, textAlign, modifier)
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MDListItems(
    listBlock: ListBlock,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
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
                    is BulletList -> MDBulletList(child, color, style, fontWeight, textAlign, modifier)
                    is OrderedList -> MDOrderedList(child, color, style, fontWeight, textAlign, modifier)
                    else -> item(child)
                }
                child = child.next
            }
            listItem = listItem.next
        }
    }
}

@Composable
private fun MDBlockQuote(blockQuote: BlockQuote, color: Color, modifier: Modifier = Modifier) {
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
            appendMarkdownChildren(blockQuote, color)
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
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier,
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is BlockQuote -> MDBlockQuote(child, color, modifier)
            is Heading -> MDHeading(child, color, style, fontWeight, textAlign, modifier)
            is Paragraph -> MDParagraph(child, color, style, fontWeight, textAlign, modifier)
            is FencedCodeBlock -> MDFencedCodeBlock(child, modifier)
            is BulletList -> MDBulletList(child, color, style, fontWeight, textAlign, modifier)
            is OrderedList -> MDOrderedList(child, color, style, fontWeight, textAlign, modifier)
        }
        child = child.next
    }
}

private fun AnnotatedString.Builder.appendMarkdownChildren(
    parent: Node,
    color: Color,
) {
    var child = parent.firstChild
    while (child != null) {
        when (child) {
            is Paragraph -> appendMarkdownChildren(child, color)
            is Text -> append(child.literal)
            is Emphasis -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendMarkdownChildren(child, color)
                pop()
            }
            is StrongEmphasis -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendMarkdownChildren(child, color)
                pop()
            }
            is Code -> {
                pushStyle(TextStyle(fontFamily = FontFamily.Monospace).toSpanStyle())
                append(child.literal)
                pop()
            }
            is HardLineBreak -> {
                append("\n")
            }
            is Link -> {
                val underline = SpanStyle(color, textDecoration = TextDecoration.Underline)
                pushStyle(underline)
                pushStringAnnotation(TAG_URL, child.destination)
                appendMarkdownChildren(child, color)
                pop()
                pop()
            }
            is Strikethrough -> {
                pushStyle(TextStyle(textDecoration = TextDecoration.LineThrough).toSpanStyle())
                appendMarkdownChildren(child, color)
                pop()
            }
        }
        child = child.next
    }
}

@SuppressWarnings("LongParameterList")
@Composable
private fun MarkdownText(
    text: AnnotatedString,
    color: Color,
    style: TextStyle,
    fontWeight: FontWeight?,
    textAlign: TextAlign?,
    modifier: Modifier = Modifier,
) {
    val layoutResult = remember { mutableStateOf<TextLayoutResult?>(null) }

    Text(
        text = text,
        color = color,
        style = style,
        fontWeight = fontWeight,
        textAlign = textAlign,
        modifier = modifier,
        onTextLayout = { layoutResult.value = it },
    )
}
