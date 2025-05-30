package com.revenuecat.purchases.ui.revenuecatui.errors

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.PaywallComponent
import com.revenuecat.purchases.paywalls.components.common.Background
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.TabControlNotInTab.message
import com.revenuecat.purchases.ui.revenuecatui.errors.PaywallValidationError.TabsComponentWithoutTabs.message
import com.revenuecat.purchases.ui.revenuecatui.strings.PaywallValidationErrorStrings

internal sealed class PaywallValidationError : Throwable() {

    @Suppress("CyclomaticComplexMethod")
    fun associatedErrorString(offering: Offering): String {
        return when (this) {
            is InvalidIcons -> {
                val joinedInvalidIcons = this.invalidIcons.joinToString()
                PaywallValidationErrorStrings.INVALID_ICONS.format(joinedInvalidIcons)
            }
            is InvalidTemplate -> PaywallValidationErrorStrings.INVALID_TEMPLATE_NAME.format(templateName)
            is InvalidVariables -> {
                val joinedUnrecognizedVariables = this.unrecognizedVariables.joinToString()
                PaywallValidationErrorStrings.INVALID_VARIABLES.format(joinedUnrecognizedVariables)
            }
            is MissingPaywall -> PaywallValidationErrorStrings.MISSING_PAYWALL.format(offering.identifier)
            is MissingTiers -> {
                PaywallValidationErrorStrings.MISSING_TIERS.format(offering.identifier)
            }
            is MissingTierConfigurations -> {
                val joinedTierIds = this.tierIds.joinToString()
                PaywallValidationErrorStrings.MISSING_TIER_CONFIGURATIONS.format(joinedTierIds)
            }
            is MissingStringLocalization -> message
            is MissingImageLocalization -> message
            is AllLocalizationsMissing -> message
            is AllVariableLocalizationsMissing -> message
            is MissingPackage -> message
            is MissingAllPackages -> message
            is MissingColorAlias -> message
            is AliasedColorIsAlias -> message
            is MissingFontAlias -> message
            is InvalidModeForComponentsPaywall -> PaywallValidationErrorStrings.INVALID_MODE_FOR_COMPONENTS_PAYWALL
            is TabsComponentWithoutTabs -> message
            is TabControlNotInTab -> message
            is UnsupportedBackgroundType -> message
            is RootComponentUnsupportedProperties -> message
        }
    }

    object MissingPaywall : PaywallValidationError()
    data class InvalidTemplate(val templateName: String) : PaywallValidationError()
    data class InvalidVariables(val unrecognizedVariables: Set<String>) : PaywallValidationError()
    data class InvalidIcons(val invalidIcons: Set<String>) : PaywallValidationError()
    object MissingTiers : PaywallValidationError()
    data class MissingTierConfigurations(val tierIds: Set<String>) : PaywallValidationError()
    data class MissingStringLocalization(
        val key: LocalizationKey,
        val locale: LocaleId? = null,
    ) : PaywallValidationError() {
        override val message: String = locale?.let {
            PaywallValidationErrorStrings.MISSING_STRING_LOCALIZATION_WITH_LOCALE.format(key.value, locale.value)
        } ?: PaywallValidationErrorStrings.MISSING_STRING_LOCALIZATION.format(key.value)
    }
    data class MissingImageLocalization(
        val key: LocalizationKey,
        val locale: LocaleId? = null,
    ) : PaywallValidationError() {
        override val message: String = locale?.let {
            PaywallValidationErrorStrings.MISSING_IMAGE_LOCALIZATION_WITH_LOCALE.format(key.value, locale.value)
        } ?: PaywallValidationErrorStrings.MISSING_IMAGE_LOCALIZATION.format(key.value)
    }
    data class AllLocalizationsMissing(
        val locale: LocaleId,
    ) : PaywallValidationError() {
        override val message: String =
            PaywallValidationErrorStrings.ALL_LOCALIZATIONS_MISSING_FOR_LOCALE.format(locale.value)
    }
    data class AllVariableLocalizationsMissing(
        val locale: LocaleId,
    ) : PaywallValidationError() {
        override val message: String =
            PaywallValidationErrorStrings.ALL_VARIABLE_LOCALIZATIONS_MISSING_FOR_LOCALE.format(locale.value)
    }
    data class MissingPackage(
        val offeringId: String,
        val missingPackageId: String,
        val allPackageIds: Collection<String>,
    ) : PaywallValidationError() {
        override val message: String =
            PaywallValidationErrorStrings.MISSING_PACKAGE
                .format(missingPackageId, offeringId, allPackageIds.joinToString())
    }
    data class MissingAllPackages(
        val offeringId: String,
        val allPackageIds: Collection<String>,
    ) : PaywallValidationError() {
        override val message: String =
            PaywallValidationErrorStrings.MISSING_ALL_PACKAGES
                .format(offeringId, allPackageIds.joinToString())
    }
    data class MissingColorAlias(
        val alias: ColorAlias,
    ) : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.MISSING_COLOR_ALIAS.format(alias.value)
    }
    data class AliasedColorIsAlias(
        val alias: ColorAlias,
        val aliasedValue: ColorAlias,
    ) : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.ALIASED_COLOR_IS_ALIAS
            .format(alias.value, aliasedValue.value)
    }
    data class MissingFontAlias(
        val alias: FontAlias,
    ) : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.MISSING_FONT_ALIAS.format(alias.value)
    }
    object InvalidModeForComponentsPaywall : PaywallValidationError()
    object TabsComponentWithoutTabs : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.TABS_COMPONENT_WITHOUT_TABS
    }
    object TabControlNotInTab : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.TAB_CONTROL_NOT_IN_TAB
    }
    data class UnsupportedBackgroundType(
        val background: Background.Unknown,
    ) : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.UNSUPPORTED_BACKGROUND_TYPE
            .format(background.type)
    }
    data class RootComponentUnsupportedProperties(
        val component: PaywallComponent,
    ) : PaywallValidationError() {
        override val message: String = PaywallValidationErrorStrings.ROOT_COMPONENT_UNSUPPORTED_PROPERTIES
            .format(component::class.java.simpleName)
    }
}
