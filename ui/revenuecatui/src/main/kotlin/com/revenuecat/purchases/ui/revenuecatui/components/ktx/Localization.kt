package com.revenuecat.purchases.ui.revenuecatui.components.ktx

import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingImageLocalization
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.MissingStringLocalization

/**
 * Retrieves a string from this [LocalizationDictionary] associated with the provided [key].
 *
 * @return A successful result containing the string if it was found, or a failure result containing a
 * [MissingStringLocalization] error if there was no string value associated with the provided [key].
 */
@JvmSynthetic
internal fun LocalizationDictionary.string(key: LocalizationKey): Result<String> =
    (get(key) as? LocalizationData.Text)?.value
        ?.let { Result.success(it) }
        ?: Result.failure(MissingStringLocalization(key))

/**
 * Retrieves an image from this [LocalizationDictionary] associated with the provided [key].
 *
 * @return A successful result containing the image if it was found, or a failure result containing a
 * [MissingStringLocalization] error if there was no image value associated with the provided [key].
 */
@JvmSynthetic
internal fun LocalizationDictionary.image(key: LocalizationKey): Result<ThemeImageUrls> =
    (get(key) as? LocalizationData.Image)?.value
        ?.let { Result.success(it) }
        ?: Result.failure(MissingImageLocalization(key))
