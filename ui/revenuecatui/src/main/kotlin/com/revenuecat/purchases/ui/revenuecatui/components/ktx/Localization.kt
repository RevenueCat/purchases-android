@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingImageLocalization
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingStringLocalization
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyList
import com.revenuecat.purchases.ui.revenuecatui.helpers.NonEmptyMap
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapError
import com.revenuecat.purchases.ui.revenuecatui.helpers.mapValuesOrAccumulate
import com.revenuecat.purchases.ui.revenuecatui.helpers.nonEmptyListOf
import androidx.compose.ui.text.intl.Locale as ComposeLocale
import java.util.Locale as JavaLocale

internal typealias LocalizationDictionary = NonEmptyMap<LocalizationKey, LocalizationData>

/**
 * Retrieves a string from this [LocalizationDictionary] associated with the provided [key].
 *
 * @return A successful result containing the string if it was found, or an error result containing a
 * [MissingStringLocalization] error if there was no string value associated with the provided [key].
 */
@JvmSynthetic
internal fun LocalizationDictionary.string(key: LocalizationKey): Result<String, MissingStringLocalization> =
    (get(key) as? LocalizationData.Text)?.value
        ?.let { Result.Success(it) }
        ?: Result.Error(MissingStringLocalization(key))

/**
 * Retrieves a string for all locales in this map, associated with the provided [key].
 *
 * @return A successful result containing the string keyed by the locale if it was found for all locales, or an error
 * result containing a [MissingStringLocalization] error for each locale the [key] wasn't found for.
 */
@JvmSynthetic
internal fun NonEmptyMap<LocaleId, LocalizationDictionary>.stringForAllLocales(
    key: LocalizationKey,
): Result<NonEmptyMap<LocaleId, String>, NonEmptyList<MissingStringLocalization>> =
    mapValues { (locale, localizationDictionary) ->
        localizationDictionary
            .string(key)
            .mapError { nonEmptyListOf(MissingStringLocalization(key, locale)) }
    }.mapValuesOrAccumulate { it }

/**
 * Retrieves an Image for all locales in this map, associated with the provided [key].
 *
 * @return A successful result containing the image keyed by the locale if it was found for all locales, or an error
 * result containing a [MissingImageLocalization] error for each locale the [key] wasn't found for.
 */
@JvmSynthetic
internal fun NonEmptyMap<LocaleId, LocalizationDictionary>.imageForAllLocales(
    key: LocalizationKey,
): Result<NonEmptyMap<LocaleId, ThemeImageUrls>, NonEmptyList<MissingImageLocalization>> =
    mapValues { (locale, localizationDictionary) ->
        localizationDictionary
            .image(key)
            .mapError { nonEmptyListOf(MissingImageLocalization(key, locale)) }
    }.mapValuesOrAccumulate { it }

/**
 * Retrieves an image from this [LocalizationDictionary] associated with the provided [key].
 *
 * @return A successful result containing the image if it was found, or an error result containing a
 * [MissingStringLocalization] error if there was no image value associated with the provided [key].
 */
@JvmSynthetic
internal fun LocalizationDictionary.image(key: LocalizationKey): Result<ThemeImageUrls, MissingImageLocalization> =
    (get(key) as? LocalizationData.Image)?.value
        ?.let { Result.Success(it) }
        ?: Result.Error(MissingImageLocalization(key))

@JvmSynthetic
internal fun LocaleId.toComposeLocale(): ComposeLocale =
    ComposeLocale(value.replace('_', '-'))

@JvmSynthetic
internal fun LocaleId.toJavaLocale(): JavaLocale =
    JavaLocale.forLanguageTag(value.replace('_', '-'))

@JvmSynthetic
internal fun ComposeLocale.toLocaleId(): LocaleId =
    LocaleId(toLanguageTag().replace('-', '_'))

@JvmSynthetic
internal fun ComposeLocale.toJavaLocale(): JavaLocale =
    JavaLocale.forLanguageTag(toLanguageTag())

@JvmSynthetic
internal fun <V> Map<LocaleId, V>.getBestMatch(localeId: LocaleId): V? =
    keys.getBestMatch(localeId)?.let { bestMatch -> get(bestMatch) }

/**
 * Returns the best match to [localeId] in this set, or null if no match is found.
 */
@Suppress("ReturnCount")
@JvmSynthetic
internal fun Set<LocaleId>.getBestMatch(localeId: LocaleId): LocaleId? {
    // Exact match:
    if (this.contains(localeId)) return localeId

    return localeId.withFallbackLanguages().firstNotNullOfOrNull { preferredLocale ->
        val language = preferredLocale.language
        val region = preferredLocale.country
        val script = preferredLocale.script.takeUnless { it.isBlank() }
            ?: scriptByRegion[region]

        // Various permutations of the provided [localeId], from least to most specific.
        val languageId = LocaleId(language)
        val languageScriptId = script?.let { LocaleId("${language}_$script") }
        val languageScriptRegionId = script?.let { LocaleId("${language}_${script}_$region") }
        val exactId = LocaleId(preferredLocale.toLanguageTag().replace('-', '_'))

        // First see if we can find an exact match of one of our permutations:
        buildList {
            add(exactId)
            if (languageScriptRegionId != null) add(languageScriptRegionId)
            if (languageScriptId != null) add(languageScriptId)
            add(languageId)
        }.firstOrNull { id -> this.contains(id) }
            ?.also { return@firstNotNullOfOrNull it }

        // If not, try a fuzzy match by dropping the region:
        val javaLocales = this.map { it.toJavaLocale() }

        languageScriptId?.takeIf { javaLocales.any { it.language == language && it.script == script } }
            ?: languageId.takeIf { javaLocales.any { it.language == language } }
    }
}

private fun LocaleId.withFallbackLanguages(): List<JavaLocale> {
    val javaLocale = toJavaLocale()
    val fallbackLanguages = javaLocale.fallbackLanguages
    return if (fallbackLanguages.isNotEmpty()) {
        buildList {
            add(javaLocale)
            addAll(fallbackLanguages)
        }
    } else {
        listOf(javaLocale)
    }
}

/**
 * For some language codes it is expected to fall back to another language code if an exact match can't be found. This
 * is the case for Norwegian Bokmål (`nb`) and Norwegian Nynorsk (`nn`), for instance.
 */
private val JavaLocale.fallbackLanguages: List<JavaLocale>
    get() = when (language) {
        "nb" -> listOf(JavaLocale("no", country, variant))
        "nn" -> listOf(JavaLocale("no", country, variant))
        else -> emptyList()
    }

/**
 * Scripts inferred from the region.
 */
private val scriptByRegion = mapOf(
    "CN" to "Hans",
    "SG" to "Hans",
    "MY" to "Hans",
    "TW" to "Hant",
    "HK" to "Hant",
    "MO" to "Hant",
)
