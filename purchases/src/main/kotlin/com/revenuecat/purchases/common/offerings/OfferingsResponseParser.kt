package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.common.warnLog
import org.json.JSONObject

internal class RawPaywallComponents(
    private val payloadText: String,
    private val start: Int,
    private val end: Int,
    val topLevelKeys: Set<String>,
) {
    fun text(): String = payloadText.substring(start, end)
}

/**
 * [json] has every `paywall_components` object elided to `null`; [paywallComponents] holds them as raw text,
 * index-parallel to the `offerings` array (`null` for an offering without one).
 */
internal class ParsedOfferingsResponse(
    val json: JSONObject,
    val paywallComponents: List<RawPaywallComponents?>,
)

/**
 * Parses an offerings response keeping `paywall_components` as raw text instead of [JSONObject] nodes. Those
 * subtrees measured 96-99% of compact response size on components-heavy accounts.
 */
internal object OfferingsResponseParser {

    private const val NULL_LITERAL = "null"

    fun parse(payloadText: String): ParsedOfferingsResponse {
        return try {
            val scanner = Scanner(payloadText)
            scanner.parseRoot()
            ParsedOfferingsResponse(
                json = JSONObject(elide(payloadText, scanner.componentSpans)),
                paywallComponents = scanner.componentsByOfferingIndex,
            )
        } catch (@Suppress("SwallowedException") e: UnrecognizedShape) {
            warnLog { "Unrecognized offerings response shape; falling back to a full-tree parse." }
            ParsedOfferingsResponse(json = JSONObject(payloadText), paywallComponents = emptyList())
        }
    }

    private fun elide(payloadText: String, spans: List<IntRange>): String {
        if (spans.isEmpty()) return payloadText
        val elidedLength = spans.sumOf { it.last - it.first + 1 }
        val builder = StringBuilder(payloadText.length - elidedLength + spans.size * NULL_LITERAL.length)
        var cursor = 0
        for (span in spans) {
            builder.append(payloadText, cursor, span.first)
            builder.append(NULL_LITERAL)
            cursor = span.last + 1
        }
        builder.append(payloadText, cursor, payloadText.length)
        return builder.toString()
    }

    private class UnrecognizedShape : Exception()

    @Suppress("TooManyFunctions")
    private class Scanner(private val text: String) {
        private var pos = 0
        val componentSpans = mutableListOf<IntRange>()
        val componentsByOfferingIndex = mutableListOf<RawPaywallComponents?>()

        fun parseRoot() {
            skipWhitespace()
            expect('{')
            var seenOfferings = false
            parseMembers { key ->
                if (key == "offerings") {
                    // org.json keeps only the last of duplicate keys, desyncing the index-parallel list.
                    if (seenOfferings) throw UnrecognizedShape()
                    seenOfferings = true
                    parseOfferingsArray()
                } else {
                    skipValue()
                }
            }
        }

        private fun parseOfferingsArray() {
            skipWhitespace()
            expect('[')
            skipWhitespace()
            if (peekIs(']')) {
                pos++
                return
            }
            while (true) {
                parseOfferingObject()
                skipWhitespace()
                when {
                    peekIs(',') -> {
                        pos++
                        skipWhitespace()
                    }
                    peekIs(']') -> {
                        pos++
                        return
                    }
                    else -> throw UnrecognizedShape()
                }
            }
        }

        private fun parseOfferingObject() {
            skipWhitespace()
            expect('{')
            var components: RawPaywallComponents? = null
            parseMembers { key ->
                if (key == "paywall_components") {
                    if (components != null) throw UnrecognizedShape()
                    skipWhitespace()
                    val start = pos
                    if (peekIs('{')) {
                        val topLevelKeys = parseObjectCollectingTopLevelKeys()
                        components = RawPaywallComponents(text, start, pos, topLevelKeys)
                        componentSpans.add(start until pos)
                    } else {
                        skipValue()
                    }
                } else {
                    skipValue()
                }
            }
            componentsByOfferingIndex.add(components)
        }

        private fun parseObjectCollectingTopLevelKeys(): Set<String> {
            val keys = mutableSetOf<String>()
            expect('{')
            parseMembers { key ->
                keys.add(key)
                skipValue()
            }
            return keys
        }

        /** Opening brace already consumed. [onMember] must consume exactly its key's value. */
        private inline fun parseMembers(onMember: (String) -> Unit) {
            skipWhitespace()
            if (peekIs('}')) {
                pos++
                return
            }
            while (true) {
                skipWhitespace()
                val key = parseStringRaw()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                onMember(key)
                skipWhitespace()
                when {
                    peekIs(',') -> pos++
                    peekIs('}') -> {
                        pos++
                        return
                    }
                    else -> throw UnrecognizedShape()
                }
            }
        }

        private fun skipValue() {
            skipWhitespace()
            when (peek()) {
                '"' -> skipString()
                '{' -> skipObject()
                '[' -> skipArray()
                else -> skipLiteral()
            }
        }

        private fun skipObject() {
            expect('{')
            skipWhitespace()
            if (peekIs('}')) {
                pos++
                return
            }
            while (true) {
                skipWhitespace()
                skipString()
                skipWhitespace()
                expect(':')
                skipWhitespace()
                skipValue()
                skipWhitespace()
                when {
                    peekIs(',') -> pos++
                    peekIs('}') -> {
                        pos++
                        return
                    }
                    else -> throw UnrecognizedShape()
                }
            }
        }

        private fun skipArray() {
            expect('[')
            skipWhitespace()
            if (peekIs(']')) {
                pos++
                return
            }
            while (true) {
                skipValue()
                skipWhitespace()
                when {
                    peekIs(',') -> {
                        pos++
                        skipWhitespace()
                    }
                    peekIs(']') -> {
                        pos++
                        return
                    }
                    else -> throw UnrecognizedShape()
                }
            }
        }

        private fun skipLiteral() {
            val start = pos
            while (pos < text.length && !text[pos].terminatesLiteral()) pos++
            if (pos == start) throw UnrecognizedShape()
        }

        private fun Char.terminatesLiteral(): Boolean = when (this) {
            ',', '}', ']', ' ', '\t', '\n', '\r' -> true
            else -> false
        }

        /** Every key compared against this is plain ASCII, so an escaped key is an unrecognized shape. */
        private fun parseStringRaw(): String {
            expect('"')
            val start = pos
            skipStringBody()
            val raw = text.substring(start, pos - 1)
            if (raw.indexOf('\\') >= 0) throw UnrecognizedShape()
            return raw
        }

        private fun skipString() {
            expect('"')
            skipStringBody()
        }

        /** Advances from just after the opening quote to just after the closing quote. */
        private fun skipStringBody() {
            while (true) {
                if (pos >= text.length) throw UnrecognizedShape()
                when (text[pos]) {
                    '\\' -> pos += 2
                    '"' -> {
                        pos++
                        return
                    }
                    else -> pos++
                }
            }
        }

        private fun skipWhitespace() {
            while (pos < text.length && text[pos].isWhitespace()) pos++
        }

        private fun peek(): Char {
            if (pos >= text.length) throw UnrecognizedShape()
            return text[pos]
        }

        private fun peekIs(char: Char): Boolean = pos < text.length && text[pos] == char

        private fun expect(char: Char) {
            if (!peekIs(char)) throw UnrecognizedShape()
            pos++
        }
    }
}
