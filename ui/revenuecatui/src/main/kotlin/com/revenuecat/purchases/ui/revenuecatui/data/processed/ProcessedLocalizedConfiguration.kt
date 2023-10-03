package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.paywalls.PaywallData
import java.util.Locale

internal data class ProcessedLocalizedConfiguration(
    val title: String,
    val subtitle: String?,
    val callToAction: String,
    val callToActionWithIntroOffer: String?,
    val callToActionWithMultipleIntroOffers: String?,
    val offerDetails: String?,
    val offerDetailsWithIntroOffer: String?,
    val offerDetailsWithMultipleIntroOffers: String?,
    val offerName: String?,
    val features: List<PaywallData.LocalizedConfiguration.Feature> = emptyList(),
) {
    companion object {
        fun create(
            variableDataProvider: VariableDataProvider,
            localizedConfiguration: PaywallData.LocalizedConfiguration,
            rcPackage: Package,
            locale: Locale,
        ): ProcessedLocalizedConfiguration {
            fun String.processVariables(): String {
                return VariableProcessor.processVariables(
                    variableDataProvider,
                    this,
                    rcPackage,
                    locale,
                )
            }
            with(localizedConfiguration) {
                return ProcessedLocalizedConfiguration(
                    title = title.processVariables(),
                    subtitle = subtitle?.processVariables(),
                    callToAction = callToAction.processVariables(),
                    callToActionWithIntroOffer = callToActionWithIntroOffer?.processVariables(),
                    callToActionWithMultipleIntroOffers = callToActionWithMultipleIntroOffers?.processVariables(),
                    offerDetails = offerDetails?.processVariables(),
                    offerDetailsWithIntroOffer = offerDetailsWithIntroOffer?.processVariables(),
                    offerDetailsWithMultipleIntroOffers = offerDetailsWithMultipleIntroOffers?.processVariables(),
                    offerName = offerName?.processVariables(),
                    features = features.map { feature ->
                        feature.copy(
                            title = feature.title.processVariables(),
                            content = feature.content?.processVariables(),
                        )
                    },
                )
            }
        }
    }
}
