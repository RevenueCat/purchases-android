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
            return ProcessedLocalizedConfiguration(
                title = localizedConfiguration.title.processVariables(),
                subtitle = localizedConfiguration.subtitle?.processVariables(),
                callToAction = localizedConfiguration.callToAction.processVariables(),
                callToActionWithIntroOffer = localizedConfiguration.callToActionWithIntroOffer?.processVariables(),
                offerDetails = localizedConfiguration.offerDetails?.processVariables(),
                offerDetailsWithIntroOffer = localizedConfiguration.offerDetailsWithIntroOffer?.processVariables(),
                offerName = localizedConfiguration.offerName?.processVariables(),
                features = localizedConfiguration.features.map { feature ->
                    feature.copy(
                        title = feature.title.processVariables(),
                        content = feature.content?.processVariables(),
                    )
                },
            )
        }
    }
}
