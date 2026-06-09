@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.graphics.Typeface
import java.io.File

/**
 * Loads an [android.graphics.Typeface] from a local font [file]. Exposed as a seam so the platform's
 * file-based loaders can be substituted in tests.
 */
internal fun interface FileTypefaceLoader {
    fun load(file: File): Typeface?
}
