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
        ?: Result.Error(MissingStringLocalization(key)) // FIXME Add locale to this error!

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
    mapValues { (_, localizationDictionary) ->
        localizationDictionary
            .string(key)
            .mapError { nonEmptyListOf(it) }
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
        ?: Result.Error(MissingImageLocalization(key)) // FIXME Add locale to this error!

@JvmSynthetic
internal fun LocaleId.toComposeLocale(): ComposeLocale =
    ComposeLocale(value.replace('_', '-'))

@JvmSynthetic
internal fun LocaleId.toJavaLocale(): JavaLocale =
    JavaLocale.forLanguageTag(value.replace('_', '-'))

@JvmSynthetic
internal fun ComposeLocale.toLocaleId(): LocaleId =
    LocaleId(toLanguageTag().replace('-', '_'))
