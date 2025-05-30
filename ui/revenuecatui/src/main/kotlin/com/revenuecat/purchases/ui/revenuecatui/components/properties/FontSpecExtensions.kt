package com.revenuecat.purchases.ui.revenuecatui.components.properties

import android.content.res.AssetManager
import androidx.compose.ui.text.font.DeviceFontFamilyName
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.paywalls.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.flatMapError

@get:JvmSynthetic
private val GoogleFontsProvider: GoogleFont.Provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs,
)

/**
 * Retrieves a [FontSpec] from this map, and returns a [PaywallValidationError.MissingFontAlias] if it doesn't exist.
 * If you want to treat blank [FontAlias]es as a null [FontSpec], chain it with [recoverFromBlankFontAlias].
 */
@JvmSynthetic
internal fun Map<FontAlias, FontSpec>.getFontSpec(
    alias: FontAlias,
): Result<FontSpec, PaywallValidationError> =
    this[alias]
        ?.let { spec ->
            Result.Success(spec)
        } ?: Result.Error(PaywallValidationError.MissingFontAlias(alias))

/**
 * Returns a successful Result containing `null` if this is an error Result caused by a blank [FontAlias]. This
 * scenario should not happen, as our dashboard should not allow devs to publish a paywall containing a blank Font
 * Family field, but this is defensive in case it does happen.
 */
@Suppress("MaxLineLength")
@JvmSynthetic
internal fun Result<FontSpec, PaywallValidationError>.recoverFromBlankFontAlias(): Result<FontSpec?, PaywallValidationError> =
    flatMapError { error ->
        if (error is PaywallValidationError.MissingFontAlias && error.alias.value.isBlank()) {
            Result.Success(null)
        } else {
            Result.Error(error)
        }
    }

@JvmSynthetic
internal fun FontSpec.resolve(
    assets: AssetManager,
    weight: FontWeight,
    style: FontStyle,
): FontFamily = when (this) {
    is FontSpec.Resource -> FontFamily(Font(resId = id, weight = weight, style = style))
    is FontSpec.Asset -> FontFamily(Font(path = path, assetManager = assets, weight = weight, style = style))
    is FontSpec.Google -> FontFamily(
        Font(googleFont = GoogleFont(name), fontProvider = GoogleFontsProvider, weight = weight, style = style),
    )

    is FontSpec.Generic -> when (this) {
        FontSpec.Generic.SansSerif -> FontFamily.SansSerif
        FontSpec.Generic.Serif -> FontFamily.Serif
        FontSpec.Generic.Monospace -> FontFamily.Monospace
    }

    is FontSpec.System -> FontFamily(
        Font(familyName = DeviceFontFamilyName(name), weight = weight, style = style),
    )
}
