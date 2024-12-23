package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.FontSize
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides

@Suppress("LongParameterList")
@Immutable
internal class TextComponentStyle(
    @get:JvmSynthetic
    val texts: Map<LocaleId, String>,
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    val fontSize: FontSize,
    @get:JvmSynthetic
    val fontWeight: FontWeight?,
    @get:JvmSynthetic
    val fontFamily: FontFamily?,
    @get:JvmSynthetic
    val textAlign: TextAlign?,
    @get:JvmSynthetic
    val horizontalAlignment: Alignment.Horizontal,
    @get:JvmSynthetic
    val backgroundColor: ColorScheme?,
    @get:JvmSynthetic
    val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<LocalizedTextPartial>?,
) : ComponentStyle
