package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

internal val Package.isLifetime: Boolean
    get() = packageType == PackageType.LIFETIME

internal val Package.periodUnitLocalizationKey: VariableLocalizationKey?
    get() = if (isLifetime) VariableLocalizationKey.LIFETIME else product.period?.periodUnitLocalizationKey

internal val Package.periodUnitAbbreviatedLocalizationKey: VariableLocalizationKey?
    get() = if (isLifetime) {
        VariableLocalizationKey.LIFETIME
    } else {
        product.period?.periodUnitAbbreviatedLocalizationKey
    }

internal fun Package.productPeriodly(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? {
    val period = product.period

    return when {
        isLifetime -> localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.LIFETIME)
        period == null -> null
        period.value > 1 ->
            localizedVariableKeys
                .getStringOrLogError(period.periodValueWithUnitLocalizationKey)
                ?.format(period.value)

        else -> when (period.unit) {
            Period.Unit.DAY -> VariableLocalizationKey.DAILY
            Period.Unit.WEEK -> VariableLocalizationKey.WEEKLY
            Period.Unit.MONTH -> VariableLocalizationKey.MONTHLY
            Period.Unit.YEAR -> VariableLocalizationKey.YEARLY
            Period.Unit.UNKNOWN -> null
        }?.let { key -> localizedVariableKeys.getStringOrLogError(key) }
    }
}

internal fun Package.productPeriod(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? {
    val period = product.period

    return when {
        isLifetime -> localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.LIFETIME)
        period == null -> null
        period.value > 1 ->
            localizedVariableKeys
                .getStringOrLogError(period.periodValueWithUnitLocalizationKey)
                ?.format(period.value)

        else -> periodUnitLocalizationKey?.let { key -> localizedVariableKeys.getStringOrLogError(key) }
    }
}

internal fun Package.productPeriodAbbreviated(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? {
    val period = product.period

    return when {
        isLifetime -> localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.LIFETIME)
        period == null -> null
        period.value > 1 -> period.periodValueWithUnitAbbreviatedLocalizationKey?.let { key ->
            localizedVariableKeys.getStringOrLogError(key)?.format(period.value)
        }

        else -> periodUnitAbbreviatedLocalizationKey?.let { key -> localizedVariableKeys.getStringOrLogError(key) }
    }
}

internal fun Package.productPeriodWithUnit(
    localizedVariableKeys: Map<VariableLocalizationKey, String>,
): String? =
    when {
        isLifetime -> localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.LIFETIME)
        else -> product.period?.let { period ->
            localizedVariableKeys
                .getStringOrLogError(period.periodValueWithUnitLocalizationKey)
                ?.format(period.value)
        }
    }

internal fun Map<VariableLocalizationKey, String>.getStringOrLogError(
    key: VariableLocalizationKey,
): String? = get(key).also { string ->
    if (string == null) Logger.e("Could not find localized string for variable key: $key")
}
