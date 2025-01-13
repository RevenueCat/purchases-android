package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.GoogleListSerializer
import com.revenuecat.purchases.utils.serializers.URLSerializer
import dev.drewhamilton.poko.Poko
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@Suppress("LongParameterList")
@InternalRevenueCatAPI
@Poko
@Serializable
class PaywallComponentsData(
    @get:JvmSynthetic
    @SerialName("template_name")
    val templateName: String,
    @get:JvmSynthetic
    @Serializable(with = URLSerializer::class)
    @SerialName("asset_base_url")
    val assetBaseURL: URL,
    @get:JvmSynthetic
    @SerialName("components_config")
    val componentsConfig: ComponentsConfig,
    @get:JvmSynthetic
    @SerialName("components_localizations")
    val componentsLocalizations: Map<LocaleId, Map<LocalizationKey, LocalizationData>>,
    @get:JvmSynthetic
    @SerialName("default_locale")
    val defaultLocaleIdentifier: LocaleId,
    @get:JvmSynthetic
    val revision: Int = 0,
    @get:JvmSynthetic
    @Serializable(with = GoogleListSerializer::class)
    @SerialName("zero_decimal_place_countries")
    val zeroDecimalPlaceCountries: List<String> = emptyList(),
)
