package com.revenuecat.purchases.ui.revenuecatui.utils

import android.content.res.XmlResourceParser
import androidx.annotation.VisibleForTesting
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.xmlpull.v1.XmlPullParser
import kotlin.jvm.Throws

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
internal data class ParsedFont(
    val resId: Int,
    val weight: Int,
    val style: FontStyle,
)

@Suppress("NestedBlockDepth")
internal object FontFamilyXmlParser {
    private const val UNRECOGNIZED_VALUE = -1
    private const val DEFAULT_FONT_WEIGHT = 400
    private const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    private const val APP_NAMESPACE = "http://schemas.android.com/apk/res-auto"

    @Throws
    fun parse(parser: XmlResourceParser): FontFamily? {
        val parsedFonts = parseXmlData(parser)
        return if (parsedFonts.isNotEmpty()) {
            FontFamily(
                parsedFonts.map { (resId, weight, style) ->
                    Font(
                        resId = resId,
                        weight = FontWeight(weight),
                        style = style,
                    )
                },
            )
        } else {
            null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    @Throws
    internal fun parseXmlData(parser: XmlResourceParser): List<ParsedFont> {
        val parsedFonts = mutableListOf<ParsedFont>()

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (parser.name == "font") {
                        val parsedFont = parseFontData(parser)
                        if (parsedFont != null) {
                            parsedFonts.add(parsedFont)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return parsedFonts
    }

    private fun parseFontData(parser: XmlResourceParser): ParsedFont? {
        val fontResId = getFontResourceId(parser)
        if (fontResId == UNRECOGNIZED_VALUE) return null

        val fontWeight = getFontWeight(parser)
        val fontStyle = getFontStyle(parser)

        return ParsedFont(fontResId, fontWeight, fontStyle)
    }

    private fun getFontResourceId(parser: XmlResourceParser): Int {
        // Try app namespace first (http://schemas.android.com/apk/res-auto)
        var fontResId = parser.getAttributeResourceValue(
            APP_NAMESPACE,
            "font",
            UNRECOGNIZED_VALUE,
        )

        // Fallback to android namespace
        if (fontResId == UNRECOGNIZED_VALUE) {
            fontResId = parser.getAttributeResourceValue(
                ANDROID_NAMESPACE,
                "font",
                UNRECOGNIZED_VALUE,
            )
        }

        return fontResId
    }

    private fun getFontWeight(parser: XmlResourceParser): Int {
        // Try app namespace first
        var fontWeight = parser.getAttributeIntValue(
            APP_NAMESPACE,
            "fontWeight",
            UNRECOGNIZED_VALUE,
        )

        // Fallback to android namespace
        if (fontWeight == UNRECOGNIZED_VALUE) {
            fontWeight = parser.getAttributeIntValue(
                ANDROID_NAMESPACE,
                "fontWeight",
                DEFAULT_FONT_WEIGHT,
            )
        }

        return if (fontWeight == UNRECOGNIZED_VALUE) DEFAULT_FONT_WEIGHT else fontWeight
    }

    private fun getFontStyle(parser: XmlResourceParser): FontStyle {
        // Try app namespace first
        var fontStyleValue = parser.getAttributeValue(
            APP_NAMESPACE,
            "fontStyle",
        )

        // Fallback to android namespace
        if (fontStyleValue == null) {
            fontStyleValue = parser.getAttributeValue(
                ANDROID_NAMESPACE,
                "fontStyle",
            )
        }

        return when (fontStyleValue) {
            "italic" -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }
}
