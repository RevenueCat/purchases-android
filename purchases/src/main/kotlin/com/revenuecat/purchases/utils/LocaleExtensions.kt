@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.utils

import androidx.core.os.LocaleListCompat
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.errorLog
import java.util.Locale
import java.util.MissingResourceException

// This is a hack to make android studio previews work. In previews, the locale language method
// gives a different result than expected (for example, "es-es" instead of "es").
// In order to fix that, we convert the string to a locale and we parse that as a locale so the
// language and country are correctly parsed.
internal fun Locale.convertToCorrectlyFormattedLocale(): Locale {
    return toString().toLocale()
}

internal fun String.toLocale(): Locale {
    // Parsing language requires it to be in the form "en-US", even though Locale.toString() returns "en_US"
    return Locale.forLanguageTag(replace("_", "-"))
}

@SuppressWarnings("ReturnCount")
internal fun Locale.sharedLanguageCodeWith(locale: Locale): Boolean {
    return try {
        val sameLanguage = isO3Language == locale.isO3Language

        val sameScript = inferScript() == locale.inferScript()
        return sameLanguage && sameScript
    } catch (e: MissingResourceException) {
        errorLog("Locale $this or $locale can't obtain ISO3 language code ($e). Falling back to language.")
        language == locale.language
    }
}

/**
 * @return list of Locales from LocaleListCompat.getDefault()
 */
fun getDefaultLocales(): List<Locale> {
    return LocaleListCompat.getDefault().toList()
}

@Suppress("SpreadOperator")
@JvmSynthetic
@InternalRevenueCatAPI
operator fun LocaleListCompat.plus(elements: LocaleListCompat): LocaleListCompat {
    val combined = elements.toTypedArray() + elements.toTypedArray()
    return LocaleListCompat.create(*combined)
}

@Suppress("SpreadOperator")
@JvmSynthetic
@InternalRevenueCatAPI
fun LocaleListCompat.distinct(): LocaleListCompat {
    val distinct = toTypedArray().distinct().toTypedArray()
    return LocaleListCompat.create(*distinct)
}

@JvmSynthetic
@InternalRevenueCatAPI
fun LocaleListCompat.toTypedArray(): Array<Locale> {
    val array = arrayOfNulls<Locale>(size())
    forEachIndexed { index, locale -> array[index] = locale }
    @Suppress("UNCHECKED_CAST")
    return array as Array<Locale>
}

@JvmSynthetic
@InternalRevenueCatAPI
fun <R> LocaleListCompat.map(transform: (Locale) -> R): List<R> =
    mapIndexed { _, locale -> transform(locale) }

@JvmSynthetic
@InternalRevenueCatAPI
fun <R> LocaleListCompat.mapIndexed(transform: (index: Int, Locale) -> R): List<R> {
    val destination = ArrayList<R>(size())
    forEachIndexed { index, locale -> destination.add(transform(index, locale)) }
    return destination
}

@JvmSynthetic
@InternalRevenueCatAPI
fun LocaleListCompat.forEachIndexed(action: (index: Int, Locale) -> Unit) {
    val size = size()
    for (index in 0 until size) {
        val element = get(index)
        if (element != null) {
            action(index, element)
        } else {
            break
        }
    }
}

private fun Locale.inferScript(): String {
    if (!script.isNullOrEmpty()) {
        return script
    }

    return when (language) {
        // Special handling to allow zh-TW == zh-Hant
        "zh" -> when (country) {
            "TW", "HK", "MO" -> "Hant" // Traditional Chinese regions
            "CN", "SG" -> "Hans" // Simplified Chinese regions
            else -> ""
        }
        else -> ""
    }
}

private fun LocaleListCompat.toList(): List<Locale> {
    return Array(size()) {
        this.get(it)
    }.filterNotNull()
}
