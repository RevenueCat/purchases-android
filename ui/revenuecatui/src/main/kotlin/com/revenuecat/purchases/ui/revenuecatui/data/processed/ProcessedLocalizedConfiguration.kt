package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import java.util.Locale

internal data class ProcessedLocalizedConfiguration(
    val title: String,
    val subtitle: String?,
    val callToAction: String,
    val callToActionWithIntroOffer: String?,
    val offerDetails: String?,
    val offerDetailsWithIntroOffer: String?,
    val offerName: String?,
    val features: List<PaywallData.LocalizedConfiguration.Feature> = emptyList(),
) {
    companion object {
        fun create(
            localizedConfiguration: PaywallData.LocalizedConfiguration,
            rcPackage: Package,
            locale: Locale,
        ): ProcessedLocalizedConfiguration {
            return ProcessedLocalizedConfiguration(
                title = localizedConfiguration.title.processVariables(rcPackage, locale),
                subtitle = localizedConfiguration.subtitle?.processVariables(rcPackage, locale),
                callToAction = localizedConfiguration.callToAction.processVariables(rcPackage, locale),
                callToActionWithIntroOffer = localizedConfiguration.callToActionWithIntroOffer?.processVariables(
                    rcPackage,
                    locale,
                ),
                offerDetails = localizedConfiguration.offerDetails?.processVariables(rcPackage, locale),
                offerDetailsWithIntroOffer = localizedConfiguration.offerDetailsWithIntroOffer?.processVariables(
                    rcPackage,
                    locale,
                ),
                offerName = localizedConfiguration.offerName?.processVariables(rcPackage, locale),
                features = localizedConfiguration.features.map { feature ->
                    feature.copy(
                        title = feature.title.processVariables(rcPackage, locale),
                        content = feature.content?.processVariables(rcPackage, locale),
                    )
                },
            )
        }

        @Suppress("UnusedParameter")
        private fun String.processVariables(rcPackage: Package, locale: Locale): String {
            return this // TODO-PAYWALLS: Process variables
        }
    }
}
