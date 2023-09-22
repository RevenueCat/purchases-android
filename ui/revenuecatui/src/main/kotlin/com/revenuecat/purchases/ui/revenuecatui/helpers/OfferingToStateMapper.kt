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

internal sealed class PaywallValidationResult {
    data class Success(val displayablePaywall: PaywallData, val template: PaywallTemplate) : PaywallValidationResult()
    data class Error(
        val displayablePaywall: PaywallData,
        val template: PaywallTemplate,
        val error: PaywallValidationError,
    ) : PaywallValidationResult()
}

internal fun Offering.validatedPaywall(
    packageName: String,
    currentColors: Colors,
): PaywallValidationResult {
    val paywallData = this.paywall
        ?: return PaywallValidationResult.Error(
            PaywallData.createDefault(packages = availablePackages, packageName, currentColors),
            PaywallData.defaultTemplate,
            PaywallValidationError.MissingPaywall,
        )

    return when (val result = paywallData.validate()) {
        is Result.Error -> PaywallValidationResult.Error(
            PaywallData.createDefault(packages = availablePackages, packageName, currentColors),
            PaywallData.defaultTemplate,
            result.value,
        )
        is Result.Success -> PaywallValidationResult.Success(
            paywallData,
            result.value,
        )
    }
}

private fun PaywallData.validate(): Result<PaywallTemplate, PaywallValidationError> {
    val (_, localizedConfiguration) = localizedConfiguration

    localizedConfiguration.validate().takeIf { it != null }?.let { Result.Error(it) }

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
): PaywallViewState {
    val templateConfiguration = TemplateConfigurationFactory.create(
        variableDataProvider = variableDataProvider,
        mode = mode,
        validatedPaywallData = validatedPaywallData,
        packages = availablePackages,
        activelySubscribedProductIdentifiers = emptySet(), // TODO-PAYWALLS: Check for active subscriptions
        template,
    )
    PaywallViewState.Loaded(
        templateConfiguration = templateConfiguration,
        selectedPackage = templateConfiguration.packages.default,
    )
}

private fun PaywallData.LocalizedConfiguration.validate(): PaywallValidationError.InvalidVariables? {
    fun String?.validateVariables(): Set<String> {
        return this?.let { VariableProcessor.validateVariables(this) } ?: emptySet()
    }

    val unrecognizedVariables: Set<String> = title.validateVariables() +
        subtitle.validateVariables() +
        callToAction.validateVariables() +
        callToActionWithIntroOffer.validateVariables() +
        offerDetails.validateVariables() +
        offerDetailsWithIntroOffer.validateVariables() +
        offerName.validateVariables() +
        features.flatMap { feature ->
            feature.title.validateVariables() + feature.content.validateVariables()
        }

    if (unrecognizedVariables.isNotEmpty()) {
        return PaywallValidationError.InvalidVariables(unrecognizedVariables)
    }

    return null
}

private fun PaywallData.validateTemplate(): PaywallTemplate? {
    return PaywallTemplate.fromId(templateName)
}
