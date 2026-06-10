@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.graphics.Typeface
import java.io.File

/**
 * Loads an [android.graphics.Typeface] from a local font [file].
 */
internal fun interface FileTypefaceLoader {
    fun load(file: File): Typeface?
}
