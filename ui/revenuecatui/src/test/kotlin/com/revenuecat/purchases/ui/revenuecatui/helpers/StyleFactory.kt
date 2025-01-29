package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.FontAlias
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.paywalls.components.common.LocaleId
import com.revenuecat.purchases.paywalls.components.common.LocalizationData
import com.revenuecat.purchases.paywalls.components.common.LocalizationKey
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.components.ktx.LocalizationDictionary
import com.revenuecat.purchases.ui.revenuecatui.components.properties.FontSpec
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory
import com.revenuecat.purchases.UiConfig as ActualUiConfig
import com.revenuecat.purchases.ui.revenuecatui.components.style.StyleFactory as ActualStyleFactory

private val defaultLocaleId = LocaleId("en_US")
private val dummyLocalizations = nonEmptyMapOf(
    defaultLocaleId to nonEmptyMapOf(LocalizationKey("dummy") to LocalizationData.Text("dummy"))
)
private val defaultVariableLocalizations = nonEmptyMapOf(defaultLocaleId to variableLocalizationKeysForEnUs())

/**
 * Same as the production-code namesake, but with parameters made optional as a convenience for testing code.
 */
@Suppress("TestFunctionName")
internal fun StyleFactory(
    localizations: NonEmptyMap<LocaleId, LocalizationDictionary> = dummyLocalizations,
    uiConfig: ActualUiConfig = UiConfig(localizations = defaultVariableLocalizations),
    fontAliases: Map<FontAlias, FontSpec> = emptyMap(),
    variableLocalizations: NonEmptyMap<LocaleId, NonEmptyMap<VariableLocalizationKey, String>> =
        defaultVariableLocalizations,
    offering: Offering = Offering(
        identifier = "identifier",
        serverDescription = "description",
        metadata = emptyMap(),
        availablePackages = emptyList(),
    ),
): StyleFactory =
    ActualStyleFactory(
        localizations = localizations,
        uiConfig = uiConfig,
        fontAliases = fontAliases,
        variableLocalizations = variableLocalizations,
        offering = offering,
    )

/**
 * This is duplicated in PreviewHelpers.kt in the main source set.
 */
internal fun variableLocalizationKeysForEnUs(): NonEmptyMap<VariableLocalizationKey, String> =
    VariableLocalizationKey.values().associateWith { key ->
        when (key) {
            VariableLocalizationKey.ANNUAL -> "annual"
            VariableLocalizationKey.ANNUAL_SHORT -> "yr"
            VariableLocalizationKey.ANNUALLY -> "annually"
            VariableLocalizationKey.DAILY -> "daily"
            VariableLocalizationKey.DAY -> "day"
            VariableLocalizationKey.DAY_SHORT -> "day"
            VariableLocalizationKey.FREE_PRICE -> "free"
            VariableLocalizationKey.MONTH -> "month"
            VariableLocalizationKey.MONTH_SHORT -> "mo"
            VariableLocalizationKey.MONTHLY -> "monthly"
            VariableLocalizationKey.NUM_DAY_FEW -> "%d days"
            VariableLocalizationKey.NUM_DAY_MANY -> "%d days"
            VariableLocalizationKey.NUM_DAY_ONE -> "%d day"
            VariableLocalizationKey.NUM_DAY_OTHER -> "%d days"
            VariableLocalizationKey.NUM_DAY_TWO -> "%d days"
            VariableLocalizationKey.NUM_DAY_ZERO -> "%d day"
            VariableLocalizationKey.NUM_MONTH_FEW -> "%d months"
            VariableLocalizationKey.NUM_MONTH_MANY -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ONE -> "%d month"
            VariableLocalizationKey.NUM_MONTH_OTHER -> "%d months"
            VariableLocalizationKey.NUM_MONTH_TWO -> "%d months"
            VariableLocalizationKey.NUM_MONTH_ZERO -> "%d month"
            VariableLocalizationKey.NUM_WEEK_FEW -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_MANY -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ONE -> "%d week"
            VariableLocalizationKey.NUM_WEEK_OTHER -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_TWO -> "%d weeks"
            VariableLocalizationKey.NUM_WEEK_ZERO -> "%d week"
            VariableLocalizationKey.NUM_YEAR_FEW -> "%d years"
            VariableLocalizationKey.NUM_YEAR_MANY -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ONE -> "%d year"
            VariableLocalizationKey.NUM_YEAR_OTHER -> "%d years"
            VariableLocalizationKey.NUM_YEAR_TWO -> "%d years"
            VariableLocalizationKey.NUM_YEAR_ZERO -> "%d year"
            VariableLocalizationKey.PERCENT -> "%d%%"
            VariableLocalizationKey.WEEK -> "week"
            VariableLocalizationKey.WEEK_SHORT -> "wk"
            VariableLocalizationKey.WEEKLY -> "weekly"
            VariableLocalizationKey.YEAR -> "year"
            VariableLocalizationKey.YEAR_SHORT -> "yr"
            VariableLocalizationKey.YEARLY -> "yearly"
        }
    }.toNonEmptyMapOrNull()!!

internal fun variableLocalizationKeysForEsMx(): NonEmptyMap<VariableLocalizationKey, String> =
    VariableLocalizationKey.values().associateWith { key ->
        when (key) {
            VariableLocalizationKey.ANNUAL -> "anual"
            VariableLocalizationKey.ANNUAL_SHORT -> "año"
            VariableLocalizationKey.ANNUALLY -> "anualmente"
            VariableLocalizationKey.DAILY -> "diario"
            VariableLocalizationKey.DAY -> "día"
            VariableLocalizationKey.DAY_SHORT -> "día"
            VariableLocalizationKey.FREE_PRICE -> "gratis"
            VariableLocalizationKey.MONTH -> "mes"
            VariableLocalizationKey.MONTH_SHORT -> "mes"
            VariableLocalizationKey.MONTHLY -> "mensual"
            VariableLocalizationKey.NUM_DAY_FEW -> "%d días"
            VariableLocalizationKey.NUM_DAY_MANY -> "%d días"
            VariableLocalizationKey.NUM_DAY_ONE -> "%d día"
            VariableLocalizationKey.NUM_DAY_OTHER -> "%d días"
            VariableLocalizationKey.NUM_DAY_TWO -> "%d días"
            VariableLocalizationKey.NUM_DAY_ZERO -> "%d día"
            VariableLocalizationKey.NUM_MONTH_FEW -> "%d meses"
            VariableLocalizationKey.NUM_MONTH_MANY -> "%d meses"
            VariableLocalizationKey.NUM_MONTH_ONE -> "%d mes"
            VariableLocalizationKey.NUM_MONTH_OTHER -> "%d meses"
            VariableLocalizationKey.NUM_MONTH_TWO -> "%d meses"
            VariableLocalizationKey.NUM_MONTH_ZERO -> "%d mes"
            VariableLocalizationKey.NUM_WEEK_FEW -> "%d semanas"
            VariableLocalizationKey.NUM_WEEK_MANY -> "%d semanas"
            VariableLocalizationKey.NUM_WEEK_ONE -> "%d semana"
            VariableLocalizationKey.NUM_WEEK_OTHER -> "%d semanas"
            VariableLocalizationKey.NUM_WEEK_TWO -> "%d semanas"
            VariableLocalizationKey.NUM_WEEK_ZERO -> "%d semana"
            VariableLocalizationKey.NUM_YEAR_FEW -> "%d años"
            VariableLocalizationKey.NUM_YEAR_MANY -> "%d años"
            VariableLocalizationKey.NUM_YEAR_ONE -> "%d año"
            VariableLocalizationKey.NUM_YEAR_OTHER -> "%d años"
            VariableLocalizationKey.NUM_YEAR_TWO -> "%d años"
            VariableLocalizationKey.NUM_YEAR_ZERO -> "%d año"
            VariableLocalizationKey.PERCENT -> "%d%%"
            VariableLocalizationKey.WEEK -> "semana"
            VariableLocalizationKey.WEEK_SHORT -> "sem"
            VariableLocalizationKey.WEEKLY -> "semanal"
            VariableLocalizationKey.YEAR -> "año"
            VariableLocalizationKey.YEAR_SHORT -> "año"
            VariableLocalizationKey.YEARLY -> "anualmente"
        }
    }.toNonEmptyMapOrNull()!!
