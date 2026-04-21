package com.revenuecat.purchases.ui.revenuecatui.utils

import android.content.res.XmlResourceParser
import androidx.compose.ui.text.font.FontStyle
import org.assertj.core.api.Assertions.assertThat
import org.intellij.lang.annotations.Language
import org.junit.Test
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader

class FontFamilyXmlParserTest {

    @Test
    fun `parseXmlData returns empty list for empty XML`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:android="http://schemas.android.com/apk/res/android">
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent)
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).isEmpty()
    }

    @Test
    fun `parseXmlData returns font data with app namespace attributes`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:app="http://schemas.android.com/apk/res-auto">
                <font app:fontStyle="normal" app:fontWeight="400" app:font="@font/regular" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent, mapOf("@font/regular" to 123))
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).hasSize(1)
        assertThat(result[0].resId).isEqualTo(123) // resId
        assertThat(result[0].weight).isEqualTo(400) // weight
        assertThat(result[0].style).isEqualTo(FontStyle.Normal) // style
    }

    @Test
    fun `parseXmlData returns font data with android namespace fallback`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:android="http://schemas.android.com/apk/res/android">
                <font android:fontStyle="italic" android:fontWeight="700" android:font="@font/bold_italic" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent, mapOf("@font/bold_italic" to 456))
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).hasSize(1)
        assertThat(result[0].resId).isEqualTo(456)
        assertThat(result[0].weight).isEqualTo(700)
        assertThat(result[0].style).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `parseXmlData returns multiple font data entries`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:app="http://schemas.android.com/apk/res-auto">
                <font app:fontStyle="normal" app:fontWeight="400" app:font="@font/regular" />
                <font app:fontStyle="italic" app:fontWeight="700" app:font="@font/italic" />
                <font app:fontStyle="normal" app:fontWeight="600" app:font="@font/semibold" />
            </font-family>"""

        val parser = TestXmlResourceParser(
            xmlContent,
            mapOf(
                "@font/regular" to 100,
                "@font/italic" to 200,
                "@font/semibold" to 300
            )
        )
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).hasSize(3)
        assertThat(result[0]).isEqualTo(ParsedFont(100, 400, FontStyle.Normal))
        assertThat(result[1]).isEqualTo(ParsedFont(200, 700, FontStyle.Italic))
        assertThat(result[2]).isEqualTo(ParsedFont(300, 600, FontStyle.Normal))
    }

    @Test
    fun `parseXmlData uses default values when attributes are missing`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:app="http://schemas.android.com/apk/res-auto">
                <font app:font="@font/regular" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent, mapOf("@font/regular" to 789))
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).hasSize(1)
        assertThat(result[0].resId).isEqualTo(789)
        assertThat(result[0].weight).isEqualTo(400) // default weight
        assertThat(result[0].style).isEqualTo(FontStyle.Normal) // default style
    }

    @Test
    fun `parseXmlData returns empty list when font resource ID is invalid`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:app="http://schemas.android.com/apk/res-auto">
                <font app:fontStyle="normal" app:fontWeight="400" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent) // no resource mapping
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).isEmpty()
    }

    @Test
    fun `parseXmlData falls back from app namespace to android namespace`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family 
                xmlns:app="http://schemas.android.com/apk/res-auto"
                xmlns:android="http://schemas.android.com/apk/res/android">
                <font android:fontStyle="normal" android:fontWeight="400" android:font="@font/regular" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent, mapOf("@font/regular" to 999))
        val result = FontFamilyXmlParser.parseXmlData(parser)

        assertThat(result).hasSize(1)
        assertThat(result[0].resId).isEqualTo(999)
        assertThat(result[0].weight).isEqualTo(400)
        assertThat(result[0].style).isEqualTo(FontStyle.Normal)
    }

    @Test
    fun `parse returns null for empty XML`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:android="http://schemas.android.com/apk/res/android">
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent)
        val result = FontFamilyXmlParser.parse(parser)

        assertThat(result).isNull()
    }

    @Test
    fun `parse returns FontFamily when fonts are parsed successfully`() {
        val xmlContent = """<?xml version="1.0" encoding="utf-8"?>
            <font-family xmlns:app="http://schemas.android.com/apk/res-auto">
                <font app:fontStyle="normal" app:fontWeight="400" app:font="@font/regular" />
            </font-family>"""

        val parser = TestXmlResourceParser(xmlContent, mapOf("@font/regular" to 123))
        val result = FontFamilyXmlParser.parse(parser)

        assertThat(result).isNotNull()
    }

    // Test implementation of XmlResourceParser that uses real XML parsing
    private class TestXmlResourceParser(
        @Language("xml") xmlContent: String,
        private val resourceMap: Map<String, Int> = emptyMap()
    ) : XmlResourceParser {
        
        private val realParser: XmlPullParser = XmlPullParserFactory.newInstance().apply {
            isNamespaceAware = true
        }.newPullParser().apply {
            setInput(StringReader(xmlContent))
        }

        override fun getAttributeResourceValue(namespace: String?, attribute: String?, defaultValue: Int): Int {
            if (attribute == "font") {
                val fontValue = getAttributeValue(namespace, attribute)
                return resourceMap[fontValue] ?: defaultValue
            }
            return defaultValue
        }

        override fun getAttributeIntValue(namespace: String?, attribute: String?, defaultValue: Int): Int {
            val value = getAttributeValue(namespace, attribute)
            return value?.toIntOrNull() ?: defaultValue
        }

        // Delegate all XmlPullParser methods to the real parser
        override fun getEventType(): Int = realParser.eventType
        override fun next(): Int = realParser.next()
        override fun getName(): String? = realParser.name
        override fun getAttributeCount(): Int = realParser.attributeCount
        override fun getAttributeName(index: Int): String? = realParser.getAttributeName(index)
        override fun getAttributeValue(index: Int): String? = realParser.getAttributeValue(index)
        override fun getAttributeValue(namespace: String?, name: String?): String? = realParser.getAttributeValue(namespace, name)

        // Unused XmlResourceParser methods - minimal implementations
        override fun close() {}
        override fun getAttributeUnsignedIntValue(namespace: String?, attribute: String?, defaultValue: Int): Int = defaultValue
        override fun getAttributeBooleanValue(namespace: String?, attribute: String?, defaultValue: Boolean): Boolean = defaultValue
        override fun getAttributeFloatValue(namespace: String?, attribute: String?, defaultValue: Float): Float = defaultValue
        override fun getAttributeListValue(namespace: String?, attribute: String?, options: Array<out String>?, defaultValue: Int): Int = defaultValue
        override fun getAttributeNameResource(index: Int): Int = 0
        override fun getAttributeResourceValue(index: Int, defaultValue: Int): Int = defaultValue
        override fun getAttributeIntValue(index: Int, defaultValue: Int): Int = defaultValue
        override fun getAttributeUnsignedIntValue(index: Int, defaultValue: Int): Int = defaultValue
        override fun getAttributeBooleanValue(index: Int, defaultValue: Boolean): Boolean = defaultValue
        override fun getAttributeFloatValue(index: Int, defaultValue: Float): Float = defaultValue
        override fun getAttributeListValue(index: Int, options: Array<out String>?, defaultValue: Int): Int = defaultValue
        override fun getIdAttribute(): String? = null
        override fun getClassAttribute(): String? = null
        override fun getIdAttributeResourceValue(defaultValue: Int): Int = defaultValue
        override fun getStyleAttribute(): Int = 0

        // Minimal XmlPullParser delegate implementations for unused methods
        override fun setFeature(name: String?, state: Boolean) {}
        override fun getFeature(name: String?): Boolean = false
        override fun setProperty(name: String?, value: Any?) {}
        override fun getProperty(name: String?): Any? = null
        override fun setInput(reader: java.io.Reader?) {}
        override fun setInput(inputStream: java.io.InputStream?, inputEncoding: String?) {}
        override fun getInputEncoding(): String? = null
        override fun defineEntityReplacementText(entityName: String?, replacementText: String?) {}
        override fun getNamespaceCount(depth: Int): Int = 0
        override fun getNamespacePrefix(pos: Int): String? = null
        override fun getNamespaceUri(pos: Int): String? = null
        override fun getNamespace(prefix: String?): String? = null
        override fun getDepth(): Int = 0
        override fun getPositionDescription(): String? = null
        override fun getLineNumber(): Int = 0
        override fun getColumnNumber(): Int = 0
        override fun isWhitespace(): Boolean = false
        override fun getText(): String? = null
        override fun getTextCharacters(holderForStartAndLength: IntArray?): CharArray? = null
        override fun getNamespace(): String? = null
        override fun getPrefix(): String? = null
        override fun isEmptyElementTag(): Boolean = false
        override fun getAttributeNamespace(index: Int): String? = null
        override fun getAttributePrefix(index: Int): String? = null
        override fun getAttributeType(index: Int): String? = null
        override fun isAttributeDefault(index: Int): Boolean = false
        override fun nextToken(): Int = 0
        override fun require(type: Int, namespace: String?, name: String?) {}
        override fun nextText(): String? = null
        override fun nextTag(): Int = 0
    }
}
