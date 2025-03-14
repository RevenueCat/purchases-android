@file:Suppress("TooManyFunctions")

package com.revenuecat.purchases.ui.revenuecatui.helpers

import androidx.compose.material3.ColorScheme
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.PaywallData
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.PaywallComponentsData
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.paywalls.components.properties.Size
import com.revenuecat.purchases.paywalls.components.properties.SizeConstraint
import com.revenuecat.purchases.ui.revenuecatui.PaywallMode
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.getBestMatch
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.properties.determineFontSpecs
import com.revenuecat.purchases.ui.revenuecatui.components.properties.toBackgroundStyles
import com.revenuecat.purchases.ui.revenuecatui.components.style.StackComponentStyle
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
import java.util.Date
import kotlin.Result
import com.revenuecat.purchases.ui.revenuecatui.helpers.Result as RcResult

@Suppress("ReturnCount")
internal fun Offering.validatedPaywall(
    currentColorScheme: ColorScheme,
    resourceProvider: ResourceProvider,
): PaywallValidationResult =
    validatePaywallComponentsDataOrNull(resourceProvider)?.let { result ->
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

@JvmSynthetic
internal fun Offering.fallbackPaywall(
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

@Suppress("MaxLineLength", "ReturnCount", "LongMethod")
internal fun Offering.validatePaywallComponentsDataOrNull(
    resourceProvider: ResourceProvider,
): RcResult<PaywallValidationResult.Components, NonEmptyList<PaywallValidationError>>? {
    val paywallComponents = paywallComponents ?: return null

    // Check that the default localization is present in the localizations map.
    val defaultLocalization = paywallComponents.data.defaultLocalization
        .errorIfNull(PaywallValidationError.AllLocalizationsMissing(paywallComponents.data.defaultLocaleIdentifier))
        .mapError { nonEmptyListOf(it) }
        .getOrElse { error -> return RcResult.Error(error) }

    // Build a NonEmptyMap of localizations, ensuring that we always have the default localization as fallback.
    val localizations = nonEmptyMapOf(
        paywallComponents.data.defaultLocaleIdentifier to defaultLocalization,
        paywallComponents.data.componentsLocalizations,
    ).mapValues { (locale, map) ->
        // We need to turn our NonEmptyMap<LocaleId, Map> into NonEmptyMap<LocaleId, NonEmptyMap>. If a certain locale
        // has an empty Map, we add an AllLocalizationsMissing error for that locale to our list of errors.
        map.toNonEmptyMapOrNull()
            .errorIfNull(PaywallValidationError.AllLocalizationsMissing(locale))
            .mapError { nonEmptyListOf(it) }
    }.mapValuesOrAccumulate { it }
        .getOrElse { error -> return RcResult.Error(error) }

    // Check that the default variable localization is present in the localizations map.
    val defaultVariableLocalization = paywallComponents.defaultVariableLocalization
        .errorIfNull(
            PaywallValidationError.AllVariableLocalizationsMissing(paywallComponents.data.defaultLocaleIdentifier),
        )
        .mapError { nonEmptyListOf(it) }
        .getOrElse { error -> return RcResult.Error(error) }

    // Build a NonEmptyMap of variable localizations, ensuring that we always have the default localization as fallback.
    val variableLocalizations = nonEmptyMapOf(
        paywallComponents.data.defaultLocaleIdentifier to defaultVariableLocalization,
        paywallComponents.uiConfig.localizations,
    ).mapValues { (locale, map) ->
        // We need to turn our NonEmptyMap<LocaleId, Map> into NonEmptyMap<LocaleId, NonEmptyMap>. If a certain locale
        // has an empty Map, we add an AllLocalizationsMissing error for that locale to our list of errors.
        map.toNonEmptyMapOrNull()
            .errorIfNull(PaywallValidationError.AllLocalizationsMissing(locale))
            .mapError { nonEmptyListOf(it) }
    }.mapValuesOrAccumulate { it }
        .getOrElse { error -> return RcResult.Error(error) }

    val colorAliases = paywallComponents.uiConfig.app.colors

    // Build a map of FontAliases to FontSpecs.
    val fontAliases: Map<FontAlias, FontSpec> =
        paywallComponents.uiConfig.app.fonts.determineFontSpecs(resourceProvider)

    // Create the StyleFactory to recursively create and validate all ComponentStyles.
    val styleFactory = StyleFactory(
        localizations = localizations,
        colorAliases = colorAliases,
        fontAliases = fontAliases,
        variableLocalizations = variableLocalizations,
        offering = this,
    )
    val config = paywallComponents.data.componentsConfig.base

    // Combine the main stack with the stickyFooter and the background, or accumulate the encountered errors.
    return zipOrAccumulate(
        first = styleFactory.create(config.stack, applyBottomWindowInsets = config.stickyFooter == null),
        second = config.stickyFooter
            ?.let { styleFactory.create(it, applyBottomWindowInsets = true) }
            .orSuccessfullyNull(),
        third = config.background.toBackgroundStyles(aliases = colorAliases),
    ) { backendRootComponentResult, stickyFooterResult, background ->
        val backendRootComponent = backendRootComponentResult.componentStyle
        val stickyFooter = stickyFooterResult?.componentStyle
        // This is a temporary hack to make the root component fill the screen. This will be removed once we have a
        // definite solution for positioning the root component.
        val rootComponent = (backendRootComponent as? StackComponentStyle)
            ?.takeIf { it.size.height == SizeConstraint.Fit }
            ?.copy(size = Size(width = SizeConstraint.Fill, height = SizeConstraint.Fill))
            ?: backendRootComponent

        PaywallValidationResult.Components(
            stack = rootComponent,
            stickyFooter = stickyFooter,
            background = background,
            locales = localizations.keys,
            zeroDecimalPlaceCountries = paywallComponents.data.zeroDecimalPlaceCountries.toSet(),
            variableConfig = paywallComponents.uiConfig.variableConfig,
            packages = backendRootComponentResult.availablePackages.merge(with = stickyFooterResult?.availablePackages),
            initialSelectedTabIndex = backendRootComponentResult.defaultTabIndex ?: stickyFooterResult?.defaultTabIndex,
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

@Suppress("LongParameterList")
internal fun Offering.toComponentsPaywallState(
    validationResult: PaywallValidationResult.Components,
    activelySubscribedProductIds: Set<String>,
    purchasedNonSubscriptionProductIds: Set<String>,
    storefrontCountryCode: String?,
    dateProvider: () -> Date,
): PaywallState.Loaded.Components {
    val showPricesWithDecimals = storefrontCountryCode?.let {
        !validationResult.zeroDecimalPlaceCountries.contains(it)
    } ?: true

    return PaywallState.Loaded.Components(
        stack = validationResult.stack,
        stickyFooter = validationResult.stickyFooter,
        background = validationResult.background,
        showPricesWithDecimals = showPricesWithDecimals,
        variableConfig = validationResult.variableConfig,
        offering = this,
        locales = validationResult.locales,
        activelySubscribedProductIds = activelySubscribedProductIds,
        purchasedNonSubscriptionProductIds = purchasedNonSubscriptionProductIds,
        dateProvider = dateProvider,
        packages = validationResult.packages,
        initialSelectedTabIndex = validationResult.initialSelectedTabIndex,
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

private val PaywallComponentsData.defaultLocalization: Map<LocalizationKey, LocalizationData>?
    get() = componentsLocalizations.getBestMatch(defaultLocaleIdentifier)

private val Offering.PaywallComponents.defaultVariableLocalization: Map<VariableLocalizationKey, String>?
    get() = uiConfig.localizations.getBestMatch(data.defaultLocaleIdentifier)
