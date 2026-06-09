@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.content.Context
import android.graphics.Typeface
import android.os.Build
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.io.File

private val DefaultFileTypefaceLoader = FileTypefaceLoader { file ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Typeface.Builder(file).build()
    } else {
        Typeface.createFromFile(file)
    }
}

/**
 * Creates a Compose [androidx.compose.ui.text.font.Font] backed by a local [file] (e.g. a downloaded font cached on
 * disk) that NEVER throws when the file fails to load.
 *
 * Compose's built-in `Font(file = ...)` produces a [FontLoadingStrategy.Blocking] font whose loader rethrows any
 * failure as `IllegalStateException("Unable to load font ...")` during text layout, crashing the app. This happens when
 * the cached file was evicted by the OS after a prior existence check, is corrupt/truncated, or cannot be decoded by
 * the platform. This font's loader catches every failure and returns a safe fallback [Typeface] (weight/style aware on
 * API 28+, plain [Typeface.DEFAULT] below that), so a font-load failure degrades gracefully instead of crashing.
 */
@JvmSynthetic
internal fun localFileFont(
    file: File,
    weight: FontWeight,
    style: FontStyle,
    fileTypefaceLoader: FileTypefaceLoader = DefaultFileTypefaceLoader,
): AndroidFont = LocalFileFont(file, weight, style, fileTypefaceLoader)

private class LocalFileFont(
    val file: File,
    override val weight: FontWeight,
    override val style: FontStyle,
    val fileTypefaceLoader: FileTypefaceLoader,
) : AndroidFont(
    loadingStrategy = FontLoadingStrategy.Blocking,
    typefaceLoader = LocalFileFontTypefaceLoader,
    variationSettings = FontVariation.Settings(weight, style),
) {
    override fun toString(): String = "LocalFileFont(file=$file, weight=$weight, style=$style)"
}

private object LocalFileFontTypefaceLoader : AndroidFont.TypefaceLoader {

    // Not invoked for Blocking fonts, but required by the interface. Delegate to the blocking path.
    override suspend fun awaitLoad(context: Context, font: AndroidFont): Typeface =
        loadBlocking(context, font)

    @Suppress("TooGenericExceptionCaught")
    override fun loadBlocking(context: Context, font: AndroidFont): Typeface {
        val localFont = (font as? LocalFileFont) ?: run {
            Logger.w(
                "Expected font to be LocalFileFont, but was ${font::class.java.simpleName}. " +
                    "Falling back to default typeface.",
            )
            return fallbackTypeface(font)
        }
        return try {
            localFont.fileTypefaceLoader.load(localFont.file)
                ?: fallbackTypeface(localFont, reason = "loader returned null")
        } catch (e: Throwable) {
            fallbackTypeface(localFont, reason = e.message ?: e.javaClass.simpleName)
        }
    }

    private fun fallbackTypeface(font: LocalFileFont, reason: String): Typeface {
        Logger.w(
            "Failed to load downloaded font from ${font.file.path} ($reason). " +
                "Falling back to default typeface.",
        )
        return fallbackTypeface(font)
    }

    private fun fallbackTypeface(font: AndroidFont): Typeface {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            Typeface.create(Typeface.DEFAULT, font.weight.weight, font.style == FontStyle.Italic)
        } else {
            Typeface.DEFAULT
        }
    }
}
