package com.revenuecat.purchases.ui.revenuecatui.utils

import android.content.res.XmlResourceParser
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import org.xmlpull.v1.XmlPullParser

@Suppress("NestedBlockDepth")
internal object FontFamilyXMLParser {
    private const val unrecognizedValue = -1
    private const val defaultFontWeight = 400

    fun parse(parser: XmlResourceParser): FontFamily? {
        try {
            val fonts = mutableListOf<Font>()

            var eventType = parser.eventType
            while (eventType != XmlPullParser.END_DOCUMENT) {
                when (eventType) {
                    XmlPullParser.START_TAG -> {
                        if (parser.name == "font") {
                            val font = parseFont(parser)
                            if (font != null) {
                                fonts.add(font)
                            }
                        }
                    }
                }
                eventType = parser.next()
            }

            return if (fonts.isNotEmpty()) {
                FontFamily(fonts)
            } else {
                null
            }
        } catch (@Suppress("TooGenericExceptionCaught") e: Exception) {
            Logger.e("Error parsing XML font family", e)
            return null
        }
    }

    private fun parseFont(parser: XmlResourceParser): Font? {
        val fontResId = getFontResourceId(parser)
        if (fontResId == unrecognizedValue) return null

        val fontWeight = getFontWeight(parser)
        val fontStyle = getFontStyle(parser)

        return Font(
            resId = fontResId,
            weight = FontWeight(fontWeight),
            style = fontStyle,
        )
    }

    private fun getFontResourceId(parser: XmlResourceParser): Int {
        // Try app namespace first (http://schemas.android.com/apk/res-auto)
        var fontResId = parser.getAttributeResourceValue(
            "http://schemas.android.com/apk/res-auto",
            "font",
            unrecognizedValue,
        )

        // Fallback to android namespace
        if (fontResId == unrecognizedValue) {
            fontResId = parser.getAttributeResourceValue(
                "http://schemas.android.com/apk/res/android",
                "font",
                unrecognizedValue,
            )
        }

        return fontResId
    }

    private fun getFontWeight(parser: XmlResourceParser): Int {
        // Try app namespace first
        var fontWeight = parser.getAttributeIntValue(
            "http://schemas.android.com/apk/res-auto",
            "fontWeight",
            unrecognizedValue,
        )

        // Fallback to android namespace
        if (fontWeight == unrecognizedValue) {
            fontWeight = parser.getAttributeIntValue(
                "http://schemas.android.com/apk/res/android",
                "fontWeight",
                defaultFontWeight,
            )
        }

        return if (fontWeight == unrecognizedValue) defaultFontWeight else fontWeight
    }

    private fun getFontStyle(parser: XmlResourceParser): FontStyle {
        // Try app namespace first
        var fontStyleValue = parser.getAttributeValue(
            "http://schemas.android.com/apk/res-auto",
            "fontStyle",
        )

        // Fallback to android namespace
        if (fontStyleValue == null) {
            fontStyleValue = parser.getAttributeValue(
                "http://schemas.android.com/apk/res/android",
                "fontStyle",
            )
        }

        return when (fontStyleValue) {
            "italic" -> FontStyle.Italic
            else -> FontStyle.Normal
        }
    }
}
