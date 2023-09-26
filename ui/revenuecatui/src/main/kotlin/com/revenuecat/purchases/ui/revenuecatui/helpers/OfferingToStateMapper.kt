package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.material.Colors
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewMode
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallViewState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfigurationFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.createDefault
import com.revenuecat.purchases.ui.revenuecatui.extensions.createDefaultForIdentifiers
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultTemplate

@Suppress("ReturnCount")
internal fun Offering.validatedPaywall(
    currentColors: Colors,
): PaywallValidationResult {
    val paywallData = this.paywall
        ?: return PaywallValidationResult(
            PaywallData.createDefault(packages = availablePackages, currentColors),
            PaywallData.defaultTemplate,
            PaywallValidationError.MissingPaywall,
        )

    val template = paywallData.validate().getOrElse {
        return PaywallValidationResult(
            PaywallData.createDefaultForIdentifiers(paywallData.config.packages, currentColors),
            PaywallData.defaultTemplate,
            it as PaywallValidationError,
        )
    }
    return PaywallValidationResult(
        paywallData,
        template,
    )
}

@Suppress("ReturnCount")
private fun PaywallData.validate(): Result<PaywallTemplate> {
    val (_, localizedConfiguration) = localizedConfiguration

    val invalidVariablesError = localizedConfiguration.validateVariables()
    if (invalidVariablesError != null) {
        return Result.failure(invalidVariablesError)
    }

    val template = validateTemplate() ?: return Result.failure(PaywallValidationError.InvalidTemplate(templateName))

    // TODO-PAYWALLS: Validate icons

    return Result.success(template)
}

@Suppress("ReturnCount", "TooGenericExceptionCaught")
internal fun Offering.toPaywallViewState(
    variableDataProvider: VariableDataProvider,
    mode: PaywallViewMode,
    validatedPaywallData: PaywallData,
    template: PaywallTemplate,
): PaywallViewState.Loaded {
    val templateConfiguration = TemplateConfigurationFactory.create(
        variableDataProvider = variableDataProvider,
        mode = mode,
        validatedPaywallData = validatedPaywallData,
        packages = availablePackages,
        activelySubscribedProductIdentifiers = emptySet(), // TODO-PAYWALLS: Check for active subscriptions
        template,
    )
    return PaywallViewState.Loaded(
        templateConfiguration = templateConfiguration,
        selectedPackage = templateConfiguration.packages.default,
    )
}

private fun PaywallData.LocalizedConfiguration.validateVariables(): PaywallValidationError.InvalidVariables? {
    fun String?.validateVariablesInProperty(): Set<String> {
        return this?.let { VariableProcessor.validateVariables(this) } ?: emptySet()
    }

    val unrecognizedVariables: Set<String> = title.validateVariablesInProperty() +
        subtitle.validateVariablesInProperty() +
        callToAction.validateVariablesInProperty() +
        callToActionWithIntroOffer.validateVariablesInProperty() +
        offerDetails.validateVariablesInProperty() +
        offerDetailsWithIntroOffer.validateVariablesInProperty() +
        offerName.validateVariablesInProperty() +
        features.flatMap { feature ->
            feature.title.validateVariablesInProperty() + feature.content.validateVariablesInProperty()
        }

    if (unrecognizedVariables.isNotEmpty()) {
        return PaywallValidationError.InvalidVariables(unrecognizedVariables)
    }

    return null
}

private fun PaywallData.validateTemplate(): PaywallTemplate? {
    return PaywallTemplate.fromId(templateName)
}
