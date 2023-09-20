package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationException

internal fun PaywallData.validate() {
    validateLocalization(localizedConfiguration)
}

internal fun validateLocalization(
    localization: PaywallData.LocalizedConfiguration,
) {
    fun String.validateVariables(): Set<String> {
        return VariableProcessor.validateVariables(this)
    }

    val unrecognizedVariables = localization.title.validateVariables() +
        localization.subtitle?.validateVariables() +
        localization.callToAction.validateVariables() +
        localization.callToActionWithIntroOffer?.validateVariables() +
        localization.offerDetails?.validateVariables() +
        localization.offerDetailsWithIntroOffer?.validateVariables() +
        localization.offerName?.validateVariables() +
        localization.features.flatMap { feature ->
            listOf(
                feature.title.validateVariables(),
                feature.content?.validateVariables(),
            )
        }

    if (unrecognizedVariables.isNotEmpty()) {
        throw PaywallValidationException(
            "Found unrecognized variables: ${unrecognizedVariables.joinToString(", ")}",
        )
    }
}
