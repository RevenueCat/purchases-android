package com.revenuecat.purchases.ui.revenuecatui.components

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationDictionary
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.ThemeImageUrls

// FIXME Put these errors somewhere, and put the strings in OfferingStrings.kt?

@JvmSynthetic
internal fun LocalizationDictionary.string(key: LocalizationKey): Result<String> =
    (get(key) as? LocalizationData.Text)?.value
        ?.let { Result.success(it) }
        ?: Result.failure(
            PurchasesException(
                PurchasesError(
                    code = PurchasesErrorCode.ConfigurationError,
                    underlyingErrorMessage = "Missing string localization for property with id: ${key.value}",
                ),
            ),
        )

@JvmSynthetic
internal fun LocalizationDictionary.image(key: LocalizationKey): Result<ThemeImageUrls> =
    (get(key) as? LocalizationData.Image)?.value
        ?.let { Result.success(it) }
        ?: Result.failure(
            PurchasesException(
                PurchasesError(
                    code = PurchasesErrorCode.ConfigurationError,
                    underlyingErrorMessage = "Missing image localization for property with id: ${key.value}",
                ),
            ),
        )
