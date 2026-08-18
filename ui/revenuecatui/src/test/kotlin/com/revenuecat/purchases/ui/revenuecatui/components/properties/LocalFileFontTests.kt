package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.content.Context
import android.graphics.Typeface
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
class LocalFileFontTests {

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val anyFile = File("/data/user/0/com.example/cache/rc_paywall_fonts/font.otf")

    @Test
    fun `localFileFont preserves weight and style`() {
        val font = localFileFont(file = anyFile, weight = FontWeight(600), style = FontStyle.Italic)

        assertThat(font.weight).isEqualTo(FontWeight(600))
        assertThat(font.style).isEqualTo(FontStyle.Italic)
    }

    @Test
    fun `loadBlocking returns the loaded typeface on success`() {
        val expected = Typeface.DEFAULT_BOLD
        val font = localFileFont(
            file = anyFile,
            weight = FontWeight(400),
            style = FontStyle.Normal,
            fileTypefaceLoader = { expected },
        )

        val result = font.loadBlocking()

        assertThat(result).isSameAs(expected)
    }

    @Test
    fun `loadBlocking falls back without throwing when the loader throws`() {
        val font = localFileFont(
            file = anyFile,
            weight = FontWeight(600),
            style = FontStyle.Normal,
            fileTypefaceLoader = { throw IllegalStateException("corrupt font") },
        )

        val result = font.loadBlocking()

        assertThat(result).isNotNull
    }

    @Test
    fun `loadBlocking falls back without throwing when the loader returns null`() {
        val font = localFileFont(
            file = anyFile,
            weight = FontWeight(600),
            style = FontStyle.Normal,
            fileTypefaceLoader = { null },
        )

        val result = font.loadBlocking()

        assertThat(result).isNotNull
    }

    @Test
    @Config(sdk = [24])
    fun `loadBlocking fallback is Typeface DEFAULT below API 28`() {
        val font = localFileFont(
            file = anyFile,
            weight = FontWeight(600),
            style = FontStyle.Normal,
            fileTypefaceLoader = { throw RuntimeException("boom") },
        )

        val result = font.loadBlocking()

        assertThat(result).isSameAs(Typeface.DEFAULT)
    }

    @Test
    @Config(sdk = [28])
    fun `loadBlocking falls back without throwing on API 28 and up`() {
        val font = localFileFont(
            file = anyFile,
            weight = FontWeight(600),
            style = FontStyle.Italic,
            fileTypefaceLoader = { throw RuntimeException("boom") },
        )

        val result = font.loadBlocking()

        assertThat(result).isNotNull
    }

    private fun AndroidFont.loadBlocking(): Typeface =
        typefaceLoader.loadBlocking(context, this)!!
}
