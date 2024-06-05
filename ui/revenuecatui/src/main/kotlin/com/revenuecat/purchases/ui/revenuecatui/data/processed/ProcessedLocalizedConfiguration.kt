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
    val offerBadge: String?,
    val features: List<PaywallData.LocalizedConfiguration.Feature> = emptyList(),
    val tierName: String?,
) {
    companion object {
        fun create(
            variableDataProvider: VariableDataProvider,
            context: VariableProcessor.PackageContext,
            localizedConfiguration: PaywallData.LocalizedConfiguration,
            rcPackage: Package,
            locale: Locale,
        ): ProcessedLocalizedConfiguration {
            fun String.processVariables(): String {
                return VariableProcessor.processVariables(
                    variableDataProvider,
                    context,
                    this,
                    rcPackage,
                    locale,
                )
            }
            with(localizedConfiguration) {
                val offerOverride = offerOverrides[rcPackage.identifier]

                val offerBadge = offerOverride?.offerBadge ?: context.discountRelativeToMostExpensivePerMonth?.let {
                    variableDataProvider.localizedRelativeDiscount(it)
                }

                return ProcessedLocalizedConfiguration(
                    title = title.processVariables(),
                    subtitle = subtitle?.processVariables(),
                    callToAction = callToAction.processVariables(),
                    callToActionWithIntroOffer = callToActionWithIntroOffer?.processVariables(),
                    callToActionWithMultipleIntroOffers = callToActionWithMultipleIntroOffers?.processVariables(),
                    offerDetails = (
                        offerOverride?.offerDetails
                            ?: offerDetails
                        )?.processVariables(),
                    offerDetailsWithIntroOffer = (
                        offerOverride?.offerDetailsWithIntroOffer
                            ?: offerDetailsWithIntroOffer
                        )?.processVariables(),
                    offerDetailsWithMultipleIntroOffers = (
                        offerOverride?.offerDetailsWithMultipleIntroOffers
                            ?: offerDetailsWithMultipleIntroOffers
                        )?.processVariables(),
                    offerName = (
                        offerOverride?.offerName
                            ?: offerName
                        )?.processVariables(),
                    offerBadge = offerBadge,
                    features = features.map { feature ->
                        feature.copy(
                            title = feature.title.processVariables(),
                            content = feature.content?.processVariables(),
                        )
                    },
                    tierName = tierName,
                )
            }
        }
    }
}
