package com.revenuecat.purchases.paywalls.components.common

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.utils.serializers.URLSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.net.URL

@InternalRevenueCatAPI
@Serializable
internal data class PaywallComponentsData(
    @SerialName("template_name")
    val templateName: String,
    @Serializable(with = URLSerializer::class)
    @SerialName("asset_base_url")
    val assetBaseURL: URL,
    @SerialName("components_config")
    val componentsConfig: ComponentsConfig,
    @SerialName("components_localizations")
    val componentsLocalizations: Map<LocaleId, LocalizationDictionary>,
    @SerialName("default_locale")
    val defaultLocaleIdentifier: LocaleId,
    val revision: Int = 0,
)
