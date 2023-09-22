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
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultTemplate
import com.revenuecat.purchases.ui.revenuecatui.utils.Result

internal fun Offering.validatedPaywall(
    packageName: String,
    currentColors: Colors,
): PaywallValidationResult {
    val paywallData = this.paywall
        ?: return PaywallValidationResult(
            PaywallData.createDefault(packages = availablePackages, packageName, currentColors),
            PaywallData.defaultTemplate,
            PaywallValidationError.MissingPaywall,
        )

    return when (val result = paywallData.validate()) {
        is Result.Error -> PaywallValidationResult(
            PaywallData.createDefault(packages = availablePackages, packageName, currentColors),
            PaywallData.defaultTemplate,
            result.value,
        )

        is Result.Success -> PaywallValidationResult(
            paywallData,
            result.value,
            null,
        )
    }
}

private fun PaywallData.validate(): Result<PaywallTemplate, PaywallValidationError> {
    val (_, localizedConfiguration) = localizedConfiguration

    localizedConfiguration.validateVariables().takeIf { it != null }?.let { Result.Error(it) }

    val template = validateTemplate() ?: return Result.Error(PaywallValidationError.InvalidTemplate(templateName))

    // TODO-PAYWALLS: Validate icons

    return Result.Success(template)
}

@Suppress("ReturnCount", "TooGenericExceptionCaught")
internal fun Offering.toPaywallViewState(
    variableDataProvider: VariableDataProvider,
    mode: PaywallViewMode,
    validatedPaywallData: PaywallData,
    template: PaywallTemplate,
    @Suppress("UNUSED_PARAMETER") error: PaywallValidationError?,
): PaywallViewState {
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
    // TODO-PAYWALLS: Handle error
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
