package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationException

internal fun PaywallData.validate() {
    localizedConfiguration.validate()
    validateTemplate(templateName)
    // TODO-PAYWALLS: validate icons
}

private fun PaywallData.LocalizedConfiguration.validate() {
    fun String.validateVariables(): Set<String> {
        return VariableProcessor.validateVariables(this)
    }

    val unrecognizedVariables = title.validateVariables() +
        subtitle?.validateVariables() +
        callToAction.validateVariables() +
        callToActionWithIntroOffer?.validateVariables() +
        offerDetails?.validateVariables() +
        offerDetailsWithIntroOffer?.validateVariables() +
        offerName?.validateVariables() +
        features.flatMap { feature ->
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

private fun validateTemplate(templateName: String) {
    PaywallTemplate.fromId(templateName)
        ?: throw PaywallValidationException("Template not recognized: $templateName")
}
