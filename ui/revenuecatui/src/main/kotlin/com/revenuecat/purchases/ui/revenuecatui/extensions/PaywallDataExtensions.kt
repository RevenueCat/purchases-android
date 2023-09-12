package com.revenuecat.purchases.ui.revenuecatui.extensions

import android.net.Uri
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.os.ConfigurationCompat
import androidx.core.os.LocaleListCompat
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

@Composable
@ReadOnlyComposable
internal fun PaywallData.getColors(): PaywallData.Configuration.Colors {
    return config.colors.dark?.takeIf { isSystemInDarkTheme() } ?: config.colors.light
}

@Composable
@ReadOnlyComposable
internal fun PaywallData.localizedConfig(): PaywallData.LocalizedConfiguration? {
    val locale = getLocale()
    val configForLocale = configForLocale(locale)
    if (configForLocale == null) {
        Logger.e("No configuration found for locale $locale")
    }
    return configForLocale
}

@Composable
@ReadOnlyComposable
private fun getLocale(): Locale {
    val configuration = LocalConfiguration.current
    return ConfigurationCompat.getLocales(configuration).get(0) ?: LocaleListCompat.getDefault()[0] ?: Locale.ENGLISH
}

val PaywallData.Configuration.Colors.backgroundColor: Color
    get() = Color(background.colorInt)
val PaywallData.Configuration.Colors.text1Color: Color
    get() = Color(text1.colorInt)
val PaywallData.Configuration.Colors.text2Color: Color
    get() = text2?.let { Color(it.colorInt) } ?: text1Color
val PaywallData.Configuration.Colors.callToActionBackgroundColor: Color
    get() = Color(callToActionBackground.colorInt)
val PaywallData.Configuration.Colors.callToActionForegroundColor: Color
    get() = Color(callToActionForeground.colorInt)
val PaywallData.Configuration.Colors.accent1Color: Color
    get() = accent1?.let { Color(it.colorInt) } ?: callToActionForegroundColor
val PaywallData.Configuration.Colors.accent2Color: Color
    get() = accent2?.let { Color(it.colorInt) } ?: accent1Color

val PaywallData.iconUrlString: String
    get() = getUrlStringFromImage(config.images.icon)

private fun PaywallData.getUrlStringFromImage(image: String?): String {
    return Uri.parse(assetBaseURL.toString()).buildUpon().path(image ?: "").build().toString()
}
