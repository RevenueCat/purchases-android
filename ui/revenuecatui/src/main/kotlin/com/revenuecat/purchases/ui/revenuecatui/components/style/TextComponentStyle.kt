package com.revenuecat.purchases.ui.revenuecatui.components.style

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.properties.ColorScheme
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.ui.revenuecatui.components.LocalizedTextPartial
import com.revenuecat.purchases.ui.revenuecatui.components.PresentedOverrides
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap

@Suppress("LongParameterList")
@Immutable
internal class TextComponentStyle(
    @get:JvmSynthetic
    val texts: NonEmptyMap<LocaleId, String>,
    @get:JvmSynthetic
    val color: ColorScheme,
    @get:JvmSynthetic
    val fontSize: Int,
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
    override val size: Size,
    @get:JvmSynthetic
    val padding: PaddingValues,
    @get:JvmSynthetic
    val margin: PaddingValues,
    /**
     * The package any variables in [texts] should take values from. The selected package will be used if this is null.
     * If this is non-null and equal to the currently selected package, the `selected` [overrides] will be used if
     * available.
     */
    @get:JvmSynthetic
    val rcPackage: Package?,
    @get:JvmSynthetic
    val overrides: PresentedOverrides<LocalizedTextPartial>?,
) : ComponentStyle
