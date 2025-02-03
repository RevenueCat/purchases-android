package com.revenuecat.purchases.ui.revenuecatui.errors

import com.revenuecat.purchases.ColorAlias
import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
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
            is MissingPackage -> message
            is MissingColorAlias -> message
            is AliasedColorIsAlias -> message
            is MissingFontAlias -> message
            is InvalidModeForComponentsPaywall -> PaywallValidationErrorStrings.INVALID_MODE_FOR_COMPONENTS_PAYWALL
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
    data class MissingPackage(
        val offeringId: String,
        val packageId: String,
    ) : PaywallValidationError() {
        override val message: String =
            PaywallValidationErrorStrings.ALL_LOCALIZATIONS_MISSING_FOR_LOCALE.format(offeringId, packageId)
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
}
