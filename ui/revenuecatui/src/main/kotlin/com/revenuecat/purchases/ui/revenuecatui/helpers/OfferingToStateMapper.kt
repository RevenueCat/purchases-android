@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.material3.ColorScheme
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallAction
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.ui.revenuecatui.composables.PaywallIconName
import com.revenuecat.purchases.ui.revenuecatui.data.PaywallState
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PackageConfigurationType
import com.revenuecat.purchases.ui.revenuecatui.data.processed.PaywallTemplate
import com.revenuecat.purchases.ui.revenuecatui.data.processed.TemplateConfigurationFactory
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableDataProvider
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError
import com.revenuecat.purchases.ui.revenuecatui.extensions.createDefault
import com.revenuecat.purchases.ui.revenuecatui.extensions.createDefaultForIdentifiers
import com.revenuecat.purchases.ui.revenuecatui.extensions.defaultTemplate
import kotlin.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result as RcResult

@Suppress("ReturnCount")
internal fun Offering.validatedPaywall(
    currentColorScheme: ColorScheme,
    resourceProvider: ResourceProvider,
): PaywallValidationResult =
    paywallComponents?.validate()?.let { result ->
        // We need to either unwrap the success value, or wrap the errors in a fallback Paywall.
        when (result) {
            is RcResult.Success -> result.value
            is RcResult.Error -> fallbackPaywall(currentColorScheme, resourceProvider, errors = result.value)
        }
    } ?: paywall?.validate(currentColorScheme, resourceProvider)
        ?: fallbackPaywall(currentColorScheme, resourceProvider, error = PaywallValidationError.MissingPaywall)

internal fun PaywallData.validate(
    currentColorScheme: ColorScheme,
    resourceProvider: ResourceProvider,
): PaywallValidationResult.Legacy {
    val template = validate().getOrElse {
        return PaywallValidationResult.Legacy(
            PaywallData.createDefaultForIdentifiers(
                config.packageIds,
                currentColorScheme,
                resourceProvider,
            ),
            PaywallData.defaultTemplate,
            it as PaywallValidationError,
        )
    }
    return PaywallValidationResult.Legacy(
        this,
        template,
    )
}

private fun Offering.fallbackPaywall(
    currentColorScheme: ColorScheme,
    resourceProvider: ResourceProvider,
    error: PaywallValidationError,
): PaywallValidationResult.Legacy =
    fallbackPaywall(
        currentColorScheme = currentColorScheme,
        resourceProvider = resourceProvider,
        errors = nonEmptyListOf(error),
    )

private fun Offering.fallbackPaywall(
    currentColorScheme: ColorScheme,
    resourceProvider: ResourceProvider,
    errors: NonEmptyList<PaywallValidationError>,
): PaywallValidationResult.Legacy =
    PaywallValidationResult.Legacy(
        PaywallData.createDefault(
            availablePackages,
            currentColorScheme,
            resourceProvider,
        ),
        PaywallData.defaultTemplate,
        errors,
    )

@Suppress("MaxLineLength")
internal fun PaywallComponentsData.validate(): RcResult<PaywallValidationResult.Components, NonEmptyList<PaywallValidationError>> =
    defaultLocalization
        // Check that the default localization is present in the localizations map.
        .errorIfNull(PaywallValidationError.AllLocalizationsMissing(defaultLocaleIdentifier))
        .mapError { nonEmptyListOf(it) }
        .map { defaultLocalization ->
            // Build a NonEmptyMap, ensuring that we always have the default localization as fallback.
            nonEmptyMapOf(defaultLocaleIdentifier to defaultLocalization, componentsLocalizations)
        }
        .flatMap { localizations ->
            // We need to turn our NonEmptyMap<LocaleId, Map> into NonEmptyMap<LocaleId, NonEmptyMap>.
            localizations.mapValues { (locale, map) ->
                map.toNonEmptyMapOrNull()
                    .errorIfNull(PaywallValidationError.AllLocalizationsMissing(locale))
                    .mapError { nonEmptyListOf(it) }
            }.mapValuesOrAccumulate { it }
        }.flatMap { localizations ->
            // Use the StyleFactory to recursively create and validate all ComponentStyles.
            val styleFactory = StyleFactory(localizations)
            val actionHandler: suspend (PaywallAction) -> Unit = { /* TODO Move the action handler to the UI layer. */ }
            val config = componentsConfig.base
            zipOrAccumulate(
                styleFactory.create(config.stack, actionHandler),
                config.stickyFooter?.let { styleFactory.create(it, actionHandler) }.orSuccessfullyNull(),
            ) { stack, stickyFooter ->
                PaywallValidationResult.Components(
                    stack = stack,
                    stickyFooter = stickyFooter,
                    background = config.background,
                    locales = localizations.keys,
                )
            }
        }

@Suppress("ReturnCount")
private fun PaywallData.validate(): Result<PaywallTemplate> {
    val template = validateTemplate() ?: return Result.failure(PaywallValidationError.InvalidTemplate(templateName))

    when (template.configurationType) {
        PackageConfigurationType.SINGLE, PackageConfigurationType.MULTIPLE -> {
            val (_, localizedConfiguration) = localizedConfiguration

            val error = localizedConfiguration.validate()
            if (error != null) {
                return Result.failure(error)
            }
        }
        PackageConfigurationType.MULTITIER -> {
            // Step 1: Validate tiers in config is not empty
            val tiers = config.tiers ?: emptyList()
            if (tiers.isEmpty()) {
                return Result.failure(PaywallValidationError.MissingTiers)
            }

            // Step 2: Validate all tiers is in colors
            val tierIds = tiers.map { it.id }.toSet()
            tierIds.getMissingElements(config.colorsByTier?.keys)?.let {
                return Result.failure(
                    PaywallValidationError.MissingTierConfigurations(it),
                )
            }

            // Images are optional so just logging if they are missing
            tierIds.getMissingElements(config.imagesByTier?.keys)?.let {
                Logger.w("Missing images for tier(s): ${it.joinToString(",")}")
            }

            // Step 3: Validate all tiers are in localizations
            val (_, localizedConfigurationByTier) = tieredLocalizedConfiguration
            tierIds.getMissingElements(localizedConfigurationByTier.keys)?.let {
                return Result.failure(
                    PaywallValidationError.MissingTierConfigurations(it),
                )
            }

            // Step 4: Validate all localizations
            localizedConfigurationByTier.entries.forEach { (_, localizedConfiguration) ->
                val error = localizedConfiguration.validate()
                if (error != null) {
                    return Result.failure(error)
                }
            }
        }
    }

    return Result.success(template)
}

private fun <T> Set<T>.getMissingElements(set: Set<T>?): Set<T>? {
    val missingElements = this - (set ?: emptySet()).toSet()
    return if (missingElements.isNotEmpty()) {
        missingElements
    } else {
        null
    }
}

@Suppress("ReturnCount")
private fun PaywallData.LocalizedConfiguration.validate(): PaywallValidationError? {
    val invalidVariablesError = this.validateVariables()
    if (invalidVariablesError != null) {
        return invalidVariablesError
    }

    val invalidIconsError = this.validateIcons()
    if (invalidIconsError != null) {
        return invalidIconsError
    }

    return null
}

@Suppress("ReturnCount", "TooGenericExceptionCaught", "LongParameterList")
internal fun Offering.toLegacyPaywallState(
    variableDataProvider: VariableDataProvider,
    activelySubscribedProductIdentifiers: Set<String>,
    nonSubscriptionProductIdentifiers: Set<String>,
    mode: PaywallMode,
    validatedPaywallData: PaywallData,
    template: PaywallTemplate,
    shouldDisplayDismissButton: Boolean,
    storefrontCountryCode: String?,
): PaywallState {
    val createTemplateConfigurationResult = TemplateConfigurationFactory.create(
        variableDataProvider = variableDataProvider,
        mode = mode,
        paywallData = validatedPaywallData,
        availablePackages = availablePackages,
        activelySubscribedProductIdentifiers = activelySubscribedProductIdentifiers,
        nonSubscriptionProductIdentifiers = nonSubscriptionProductIdentifiers,
        template,
        storefrontCountryCode = storefrontCountryCode,
    )
    val templateConfiguration = createTemplateConfigurationResult.getOrElse {
        return PaywallState.Error(it.message ?: "Unknown error")
    }

    return PaywallState.Loaded.Legacy(
        offering = this,
        templateConfiguration = templateConfiguration,
        selectedPackage = templateConfiguration.packages.default,
        shouldDisplayDismissButton = shouldDisplayDismissButton,
    )
}

internal fun Offering.toComponentsPaywallState(
    validationResult: PaywallValidationResult.Components,
): PaywallState.Loaded.Components =
    PaywallState.Loaded.Components(
        stack = validationResult.stack,
        stickyFooter = validationResult.stickyFooter,
        background = validationResult.background,
        offering = this,
        locales = validationResult.locales,
    )

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

private val PaywallComponentsData.defaultLocalization: Map<LocalizationKey, LocalizationData>?
    get() = componentsLocalizations[defaultLocaleIdentifier]
