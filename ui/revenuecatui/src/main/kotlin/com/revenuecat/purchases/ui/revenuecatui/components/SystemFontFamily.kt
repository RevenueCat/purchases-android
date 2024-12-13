@file:JvmSynthetic

package com.revenuecat.purchases.ui.revenuecatui.components

import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight

/**
 * Get an Android system-installed font by [familyName].
 */
@Suppress("FunctionName")
@JvmSynthetic
internal fun SystemFontFamily(familyName: String, weight: FontWeight?): FontFamily =
    FontFamily(
        Font(
            familyName = DeviceFontFamilyName(familyName),
            weight = weight ?: FontWeight.Normal,
        ),
    )
