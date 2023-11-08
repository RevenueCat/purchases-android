package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.material3.ColorScheme
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
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
    currentColorScheme: ColorScheme,
    applicationContext: ApplicationContext,
): PaywallValidationResult {
    val paywallData = this.paywall
        ?: return PaywallValidationResult(
            PaywallData.createDefault(
                availablePackages,
                currentColorScheme,
                applicationContext,
            ),
            PaywallData.defaultTemplate,
            PaywallValidationError.MissingPaywall,
        )

    val template = paywallData.validate().getOrElse {
        return PaywallValidationResult(
            PaywallData.createDefaultForIdentifiers(
                paywallData.config.packageIds,
                currentColorScheme,
                applicationContext,
            ),
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

    val invalidIconsError = localizedConfiguration.validateIcons()
    if (invalidIconsError != null) {
        return Result.failure(invalidIconsError)
    }

    return Result.success(template)
}

@Suppress("ReturnCount", "TooGenericExceptionCaught", "LongParameterList")
internal fun Offering.toPaywallState(
    variableDataProvider: VariableDataProvider,
    activelySubscribedProductIdentifiers: Set<String>,
    nonSubscriptionProductIdentifiers: Set<String>,
    mode: PaywallMode,
    validatedPaywallData: PaywallData,
    template: PaywallTemplate,
    shouldDisplayDismissButton: Boolean,
): PaywallState {
    val createTemplateConfigurationResult = TemplateConfigurationFactory.create(
        variableDataProvider = variableDataProvider,
        mode = mode,
        paywallData = validatedPaywallData,
        availablePackages = availablePackages,
        activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
        nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
        template,
    )
    val templateConfiguration = createTemplateConfigurationResult.getOrElse {
        return PaywallState.Error(it.message ?: "Unknown error")
    }
    return PaywallState.Loaded(
        offering = this,
        templateConfiguration = templateConfiguration,
        selectedPackage = templateConfiguration.packages.default,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
    )
}

/**
 * Returns an error if any of the variables are invalid, or null if they're all valid
 */
private fun PaywallData.LocalizedConfiguration.validateVariables(): PaywallValidationError.InvalidVariables? {
    /**
     * Returns the unrecognized variables in the String
     */
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

/**
 * Returns an error if any of the icons are invalid, or null if they're all valid
 */
private fun PaywallData.LocalizedConfiguration.validateIcons(): PaywallValidationError.InvalidIcons? {
    /**
     * Returns the iconID if it's not a valid [PaywallIconName]`
     */
    fun PaywallData.LocalizedConfiguration.Feature.validateIcon(): String? {
        return this.iconID?.takeIf { PaywallIconName.fromValue(it) == null }
    }

    val invalidIcons = features.mapNotNull { feature ->
        feature.validateIcon()
    }.toSet()

    if (invalidIcons.isNotEmpty()) {
        return PaywallValidationError.InvalidIcons(invalidIcons)
    }

    return null
}

private fun PaywallData.validateTemplate(): PaywallTemplate? {
    return PaywallTemplate.fromId(templateName)
}
