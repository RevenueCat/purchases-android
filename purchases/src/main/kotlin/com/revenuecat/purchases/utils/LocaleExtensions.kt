package com.revenuecat.purchases.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.revenuecat.purchases.common.errorLog
import java.util.Locale
import java.util.MissingResourceException

// This is a hack to make android studio previews work. In previews, the locale language method
// gives a different result than expected (for example, "es-es" instead of "es").
// In order to fix that, we convert the string to a locale and we parse that as a locale so the
// language and country are correctly parsed.
@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal fun Locale.convertToCorrectlyFormattedLocale(): Locale {
    return toString().toLocale()
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal fun String.toLocale(): Locale {
    // Parsing language requires it to be in the form "en-US", even though Locale.toString() returns "en_US"
    return Locale.forLanguageTag(replace("_", "-"))
}

internal fun Locale.sharedLanguageCodeWith(locale: Locale): Boolean {
    return try {
        val sameLanguage = isO3Language == locale.isO3Language

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val sameScript = inferScript() == locale.inferScript()
            return sameLanguage && sameScript
        } else {
            return sameLanguage
        }
    } catch (e: MissingResourceException) {
        errorLog("Locale $this or $locale can't obtain ISO3 language code ($e). Falling back to language.")
        language == locale.language
    }
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
private fun Locale.inferScript(): String {
    if (!script.isNullOrEmpty()) {
        return script
    }

    // Special handling to allow zh-TW == zh-Hant
    if (language == "zh") {
        return when (country) {
            "TW", "HK", "MO" -> "Hant" // Traditional Chinese regions
            "CN", "SG" -> "Hans"       // Simplified Chinese regions
            else -> ""
        }
    }
    return ""
}
