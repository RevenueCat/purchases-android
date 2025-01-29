package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.text.DateFormat
import java.util.Calendar
import java.util.Currency
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("TooManyFunctions")
internal object VariableProcessorV2 {

    internal enum class Variable(@get:JvmSynthetic val identifier: String) {

        PRODUCT_CURRENCY_CODE("product.currency_code"),
        PRODUCT_CURRENCY_SYMBOL("product.currency_symbol"),
        PRODUCT_PERIODLY("product.periodly"),
        PRODUCT_PRICE("product.price"),
        PRODUCT_PRICE_PER_PERIOD("product.price_per_period"),
        PRODUCT_PRICE_PER_PERIOD_ABBREVIATED("product.price_per_period_abbreviated"),
        PRODUCT_PRICE_PER_DAY("product.price_per_day"),
        PRODUCT_PRICE_PER_WEEK("product.price_per_week"),
        PRODUCT_PRICE_PER_MONTH("product.price_per_month"),
        PRODUCT_PRICE_PER_YEAR("product.price_per_year"),
        PRODUCT_PERIOD("product.period"),
        PRODUCT_PERIOD_ABBREVIATED("product.period_abbreviated"),
        PRODUCT_PERIOD_IN_DAYS("product.period_in_days"),
        PRODUCT_PERIOD_IN_WEEKS("product.period_in_weeks"),
        PRODUCT_PERIOD_IN_MONTHS("product.period_in_months"),
        PRODUCT_PERIOD_IN_YEARS("product.period_in_years"),
        PRODUCT_PERIOD_WITH_UNIT("product.period_with_unit"),
        PRODUCT_OFFER_PRICE("product.offer_price"),
        PRODUCT_OFFER_PRICE_PER_DAY("product.offer_price_per_day"),
        PRODUCT_OFFER_PRICE_PER_WEEK("product.offer_price_per_week"),
        PRODUCT_OFFER_PRICE_PER_MONTH("product.offer_price_per_month"),
        PRODUCT_OFFER_PRICE_PER_YEAR("product.offer_price_per_year"),
        PRODUCT_OFFER_PERIOD("product.offer_period"),
        PRODUCT_OFFER_PERIOD_ABBREVIATED("product.offer_period_abbreviated"),
        PRODUCT_OFFER_PERIOD_IN_DAYS("product.offer_period_in_days"),
        PRODUCT_OFFER_PERIOD_IN_WEEKS("product.offer_period_in_weeks"),
        PRODUCT_OFFER_PERIOD_IN_MONTHS("product.offer_period_in_months"),
        PRODUCT_OFFER_PERIOD_IN_YEARS("product.offer_period_in_years"),
        PRODUCT_OFFER_PERIOD_WITH_UNIT("product.offer_period_with_unit"),
        PRODUCT_OFFER_END_DATE("product.offer_end_date"),
        PRODUCT_SECONDARY_OFFER_PRICE("product.secondary_offer_price"),
        PRODUCT_SECONDARY_OFFER_PERIOD("product.secondary_offer_period"),
        PRODUCT_SECONDARY_OFFER_PERIOD_ABBREVIATED("product.secondary_offer_period_abbreviated"),
        PRODUCT_RELATIVE_DISCOUNT("product.relative_discount"),
        PRODUCT_STORE_PRODUCT_NAME("product.store_product_name"),
        ;

        companion object {
            private val valuesByIdentifier by lazy {
                Variable.values().associateBy { it.identifier }
            }

            fun valueOfIdentifier(identifier: String): Variable? {
                return valuesByIdentifier[identifier]
            }
        }
    }

    internal enum class Function(@get:JvmSynthetic val identifier: String) {
        LOWERCASE("lowercase"),
        UPPERCASE("uppercase"),
        CAPITALIZE("capitalize"),
        ;

        companion object {
            private val valuesByIdentifier by lazy {
                Function.values().associateBy { it.identifier }
            }

            fun valueOfIdentifier(identifier: String): Function? {
                return valuesByIdentifier[identifier]
            }
        }
    }

    private const val PERCENT_SCALE = 100f

    private val regex = "\\{\\{\\s*(.*?)\\s*\\}\\}".toRegex()

    @Suppress("LongParameterList")
    fun processVariables(
        template: String,
        // Dependencies:
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        variableConfig: UiConfig.VariableConfig,
        variableDataProvider: VariableDataProvider,
        // "Context":
        packageContext: PackageContext,
        rcPackage: Package,
        locale: Locale,
        date: Date,
    ): String {
        val resultString = handleVariablesAndReplace(template) { variable, functions ->
            variableValue(
                variableIdentifier = variable,
                functionIdentifiers = functions,
                localizedVariableKeys = localizedVariableKeys,
                variableConfig = variableConfig,
                variableDataProvider = variableDataProvider,
                packageContext = packageContext,
                rcPackage = rcPackage,
                locale = locale,
                date = date,
            )
        }
        return resultString
    }

    private fun handleVariablesAndReplace(
        string: String,
        executeAndReplaceWith: (variable: String, functions: List<String>) -> String?,
    ): String {
        var resultString = string
        regex.findAll(string).toList().reversed().forEach { matchResult ->
            val variableString = matchResult.value
            val variableWithoutBraces = variableString.substring(2, variableString.length - 2).trim()

            val parts = variableWithoutBraces.split("|").map { it.trim() }
            val variable = parts[0]
            val functions = if (parts.size > 1) parts.drop(1) else emptyList()

            val replacement = executeAndReplaceWith(variable, functions)
            replacement?.let {
                resultString = resultString.replaceRange(matchResult.range, it)
            }
        }
        return resultString
    }

    @Suppress("LongParameterList")
    private fun variableValue(
        variableIdentifier: String,
        functionIdentifiers: List<String>,
        // Dependencies:
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        variableConfig: UiConfig.VariableConfig,
        variableDataProvider: VariableDataProvider,
        // "Context":
        packageContext: PackageContext,
        rcPackage: Package,
        locale: Locale,
        date: Date,
    ): String? {
        val variable = findVariable(variableIdentifier, variableConfig.variableCompatibilityMap)
        val functions = functionIdentifiers.mapNotNull { findFunction(it, variableConfig.functionCompatibilityMap) }
        return if (variable == null) {
            Logger.e("Unknown variable: $variableIdentifier")
            null
        } else {
            return processVariable(
                variable = variable,
                localizedVariableKeys = localizedVariableKeys,
                variableDataProvider = variableDataProvider,
                packageContext = packageContext,
                rcPackage = rcPackage,
                locale = locale,
                date = date,
            )?.let { processedVariable ->
                functions.fold(processedVariable) { accumulator, function ->
                    accumulator.processFunction(function, locale)
                }
            } ?: run {
                Logger.w(
                    "Could not process value for variable '$variableIdentifier' for " +
                        "package '${rcPackage.identifier}'. Please check that the product for that package " +
                        "matches the requirements for that variable. Defaulting to empty string.",
                )
                ""
            }
        }
    }

    private fun findVariable(
        variableIdentifier: String,
        variableCompatibilityMap: Map<String, String>,
    ): Variable? {
        val original = Variable.valueOfIdentifier(variableIdentifier)
        return if (original != null) {
            original
        } else {
            val compatVariableIdentifier = variableCompatibilityMap[variableIdentifier]
            if (compatVariableIdentifier != null) {
                findVariable(compatVariableIdentifier, variableCompatibilityMap)
                    ?.also {
                        Logger.w(
                            "Paywall variable '$variableIdentifier' is not supported. Using backwards compatible " +
                                "'$compatVariableIdentifier' instead.",
                        )
                    }
            } else {
                Logger.e(
                    "Paywall variable '$variableIdentifier' is not supported and no backwards compatible " +
                        "replacement found.",
                )
                null
            }
        }
    }

    private fun findFunction(functionIdentifier: String, functionCompatibilityMap: Map<String, String>): Function? {
        val original = Function.valueOfIdentifier(functionIdentifier)
        return if (original != null) {
            original
        } else {
            val compatFunctionIdentifier = functionCompatibilityMap[functionIdentifier]
            if (compatFunctionIdentifier != null) {
                findFunction(compatFunctionIdentifier, functionCompatibilityMap)
                    ?.also {
                        Logger.w(
                            "Paywall function '$functionIdentifier' is not supported. Using backward compatible " +
                                "'$compatFunctionIdentifier' instead.",
                        )
                    }
            } else {
                Logger.e(
                    "Paywall function '$functionIdentifier' is not supported and no backwards compatible " +
                        "replacement found.",
                )
                null
            }
        }
    }

    @Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList")
    private fun processVariable(
        variable: Variable,
        // Dependencies:
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        variableDataProvider: VariableDataProvider,
        // "Context":
        packageContext: PackageContext,
        rcPackage: Package,
        locale: Locale,
        date: Date,
    ): String? = when (variable) {
        Variable.PRODUCT_CURRENCY_CODE -> rcPackage.product.price.currencyCode
        Variable.PRODUCT_CURRENCY_SYMBOL ->
            Currency
                .getInstance(rcPackage.product.price.currencyCode)
                .getSymbol(locale)

        Variable.PRODUCT_PERIODLY -> rcPackage.productPeriodly(localizedVariableKeys)

        Variable.PRODUCT_PRICE -> variableDataProvider.localizedPrice(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        )

        Variable.PRODUCT_PRICE_PER_PERIOD -> variableDataProvider.localizedPrice(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        ).let { price ->
            val period = rcPackage.productPeriod(localizedVariableKeys)
            if (period != null) "$price/$period" else null
        }

        Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED -> variableDataProvider.localizedPrice(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        ).let { price ->
            val period = rcPackage.productPeriodAbbreviated(localizedVariableKeys)
            if (period != null) "$price/$period" else null
        }

        Variable.PRODUCT_PRICE_PER_DAY -> variableDataProvider.localizedPricePerDay(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        )

        Variable.PRODUCT_PRICE_PER_WEEK -> variableDataProvider.localizedPricePerWeek(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        )

        Variable.PRODUCT_PRICE_PER_MONTH -> variableDataProvider.localizedPricePerMonth(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        )

        Variable.PRODUCT_PRICE_PER_YEAR -> variableDataProvider.localizedPricePerYear(
            rcPackage = rcPackage,
            locale = locale,
            showZeroDecimalPlacePrices = packageContext.showZeroDecimalPlacePrices,
        )

        Variable.PRODUCT_PERIOD -> rcPackage.productPeriod(localizedVariableKeys)
        Variable.PRODUCT_PERIOD_ABBREVIATED -> rcPackage.productPeriodAbbreviated(localizedVariableKeys)
        Variable.PRODUCT_PERIOD_IN_DAYS -> rcPackage.product.period?.roundedValueInDays
        Variable.PRODUCT_PERIOD_IN_WEEKS -> rcPackage.product.period?.roundedValueInWeeks
        Variable.PRODUCT_PERIOD_IN_MONTHS -> rcPackage.product.period?.roundedValueInMonths
        Variable.PRODUCT_PERIOD_IN_YEARS -> rcPackage.product.period?.roundedValueInYears
        Variable.PRODUCT_PERIOD_WITH_UNIT -> rcPackage.productPeriodWithUnit(localizedVariableKeys)
        Variable.PRODUCT_OFFER_PRICE -> rcPackage.firstIntroOffer?.productOfferPrice(localizedVariableKeys)
        Variable.PRODUCT_OFFER_PRICE_PER_DAY ->
            rcPackage.firstIntroOffer?.productOfferPricePerDay(locale, localizedVariableKeys)

        Variable.PRODUCT_OFFER_PRICE_PER_WEEK ->
            rcPackage.firstIntroOffer?.productOfferPricePerWeek(locale, localizedVariableKeys)

        Variable.PRODUCT_OFFER_PRICE_PER_MONTH ->
            rcPackage.firstIntroOffer?.productOfferPricePerMonth(locale, localizedVariableKeys)

        Variable.PRODUCT_OFFER_PRICE_PER_YEAR ->
            rcPackage.firstIntroOffer?.productOfferPricePerYear(locale, localizedVariableKeys)

        Variable.PRODUCT_OFFER_PERIOD -> rcPackage.firstIntroOffer?.productOfferPeriod(localizedVariableKeys)
        Variable.PRODUCT_OFFER_PERIOD_ABBREVIATED ->
            rcPackage.firstIntroOffer?.productOfferPeriodAbbreviated(localizedVariableKeys)

        Variable.PRODUCT_OFFER_PERIOD_IN_DAYS -> rcPackage.firstIntroOffer?.productOfferPeriodInDays
        Variable.PRODUCT_OFFER_PERIOD_IN_WEEKS -> rcPackage.firstIntroOffer?.productOfferPeriodInWeeks
        Variable.PRODUCT_OFFER_PERIOD_IN_MONTHS -> rcPackage.firstIntroOffer?.productOfferPeriodInMonths
        Variable.PRODUCT_OFFER_PERIOD_IN_YEARS -> rcPackage.firstIntroOffer?.productOfferPeriodInYears
        Variable.PRODUCT_OFFER_PERIOD_WITH_UNIT ->
            rcPackage.firstIntroOffer?.productOfferPeriodWithUnit(localizedVariableKeys)

        Variable.PRODUCT_OFFER_END_DATE -> rcPackage.firstIntroOffer?.productOfferEndDate(locale, date)
        Variable.PRODUCT_SECONDARY_OFFER_PRICE -> rcPackage.secondIntroOffer?.productOfferPrice(localizedVariableKeys)
        Variable.PRODUCT_SECONDARY_OFFER_PERIOD -> rcPackage.secondIntroOffer?.productOfferPeriod(localizedVariableKeys)
        Variable.PRODUCT_SECONDARY_OFFER_PERIOD_ABBREVIATED ->
            rcPackage.secondIntroOffer?.productOfferPeriodAbbreviated(localizedVariableKeys)

        Variable.PRODUCT_RELATIVE_DISCOUNT -> packageContext.relativeDiscount(localizedVariableKeys)

        Variable.PRODUCT_STORE_PRODUCT_NAME -> rcPackage.product.name
    }

    private fun String.processFunction(function: Function, locale: Locale): String = when (function) {
        Function.LOWERCASE -> lowercase()
        Function.UPPERCASE -> uppercase()
        // This is the recommended replacement for capitalize().
        Function.CAPITALIZE -> replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun Map<VariableLocalizationKey, String>.getStringOrLogError(
        key: VariableLocalizationKey,
    ): String? = get(key).also { string ->
        if (string == null) Logger.e("Could not find localized string for variable key: $key")
    }

    private fun Package.productPeriodly(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? =
        product.period?.let { period ->
            when (period.unit) {
                Period.Unit.DAY -> VariableLocalizationKey.DAILY
                Period.Unit.WEEK -> VariableLocalizationKey.WEEKLY
                Period.Unit.MONTH -> VariableLocalizationKey.MONTHLY
                Period.Unit.YEAR -> VariableLocalizationKey.YEARLY
                Period.Unit.UNKNOWN -> null
            }
        }?.let { key -> localizedVariableKeys.getStringOrLogError(key) }

    private fun Package.productPeriod(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? =
        product.period?.periodLocalizationKey?.let { key -> localizedVariableKeys.getStringOrLogError(key) }

    private fun Package.productPeriodAbbreviated(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        product.period?.periodAbbreviatedLocalizationKey?.let { key -> localizedVariableKeys.getStringOrLogError(key) }

    private fun Package.productPeriodWithUnit(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        product.period?.unitPeriodLocalizationKey?.let { key -> localizedVariableKeys.getStringOrLogError(key) }

    private fun PricingPhase.productOfferPrice(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        if (price.amountMicros == 0L) {
            localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
        } else {
            price.formatted
        }

    private fun PricingPhase.productOfferPricePerDay(
        locale: Locale,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.DAY) { pricePerDay(locale) }

    private fun PricingPhase.productOfferPricePerWeek(
        locale: Locale,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.WEEK) { pricePerWeek(locale) }

    private fun PricingPhase.productOfferPricePerMonth(
        locale: Locale,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.MONTH) { pricePerMonth(locale) }

    private fun PricingPhase.productOfferPricePerYear(
        locale: Locale,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        productOfferPricePerPeriod(localizedVariableKeys, Period.Unit.YEAR) { pricePerYear(locale) }

    private fun PricingPhase.productOfferPricePerPeriod(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        unit: Period.Unit,
        calculatePrice: PricingPhase.() -> Price,
    ): String? =
        takeIf { it.canDisplay(unit) }
            ?.calculatePrice()
            ?.let { offerPrice ->
                if (offerPrice.amountMicros == 0L) {
                    localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.FREE_PRICE)
                } else {
                    offerPrice.formatted
                }
            }

    private fun PricingPhase.productOfferPeriod(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? = billingPeriod.periodLocalizationKey?.let { key ->
        localizedVariableKeys.getStringOrLogError(key)
    }

    private fun PricingPhase.productOfferPeriodAbbreviated(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? = billingPeriod.periodAbbreviatedLocalizationKey?.let { key ->
        localizedVariableKeys.getStringOrLogError(key)
    }

    private fun PricingPhase.productOfferPeriodInPeriodUnit(
        unit: Period.Unit,
        calculateValue: Period.() -> String,
    ): String? =
        takeIf { it.canDisplay(unit) }?.billingPeriod?.calculateValue()

    private val PricingPhase.productOfferPeriodInDays: String?
        get() = productOfferPeriodInPeriodUnit(Period.Unit.DAY) { roundedValueInDays }

    private val PricingPhase.productOfferPeriodInWeeks: String?
        get() = productOfferPeriodInPeriodUnit(Period.Unit.WEEK) { roundedValueInWeeks }

    private val PricingPhase.productOfferPeriodInMonths: String?
        get() = productOfferPeriodInPeriodUnit(Period.Unit.MONTH) { roundedValueInMonths }

    private val PricingPhase.productOfferPeriodInYears: String?
        get() = productOfferPeriodInPeriodUnit(Period.Unit.YEAR) { roundedValueInYears }

    private fun PricingPhase.productOfferPeriodWithUnit(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? =
        localizedVariableKeys.getStringOrLogError(billingPeriod.unitPeriodLocalizationKey)?.format(billingPeriod.value)

    private fun PricingPhase.productOfferEndDate(locale: Locale, date: Date): String? {
        val futureDate = Calendar.getInstance(locale)
            .apply { time = date }
            .apply { add(Calendar.DAY_OF_YEAR, billingPeriod.valueInDays.roundToInt()) }
            .time

        return DateFormat.getDateInstance(DateFormat.LONG, locale)
            .format(futureDate)
    }

    private fun PackageContext.relativeDiscount(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? =
        discountRelativeToMostExpensivePerMonth
            ?.let { discount -> (discount * PERCENT_SCALE).roundToInt() }
            ?.let { discountPercentage ->
                localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.PERCENT)?.format(discountPercentage)
            }

    private val Package.firstIntroOffer: PricingPhase?
        get() = product.defaultOption?.let { option -> option.freePhase ?: option.introPhase }

    private val Package.secondIntroOffer: PricingPhase?
        get() = product.defaultOption?.let { option -> if (option.freePhase != null) option.introPhase else null }

    private val Period.periodLocalizationKey: VariableLocalizationKey?
        get() = when (unit) {
            Period.Unit.DAY -> VariableLocalizationKey.DAY
            Period.Unit.WEEK -> VariableLocalizationKey.WEEK
            Period.Unit.MONTH -> VariableLocalizationKey.MONTH
            Period.Unit.YEAR -> VariableLocalizationKey.YEAR
            Period.Unit.UNKNOWN -> null
        }

    private val Period.periodAbbreviatedLocalizationKey: VariableLocalizationKey?
        get() = when (unit) {
            Period.Unit.DAY -> VariableLocalizationKey.DAY_SHORT
            Period.Unit.WEEK -> VariableLocalizationKey.WEEK_SHORT
            Period.Unit.MONTH -> VariableLocalizationKey.MONTH_SHORT
            Period.Unit.YEAR -> VariableLocalizationKey.YEAR_SHORT
            Period.Unit.UNKNOWN -> null
        }

    @Suppress("MagicNumber")
    private val Period.unitPeriodLocalizationKey: VariableLocalizationKey
        get() = when {
            // Zero
            value == 0 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_ZERO
            value == 0 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_ZERO
            value == 0 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_ZERO
            value == 0 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_ZERO

            // One
            value == 1 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_ONE
            value == 1 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_ONE
            value == 1 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_ONE
            value == 1 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_ONE

            // Two
            value == 2 && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_TWO
            value == 2 && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_TWO
            value == 2 && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_TWO
            value == 2 && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_TWO

            // Few (3..4)
            (value in 3..4) && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_FEW
            (value in 3..4) && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_FEW
            (value in 3..4) && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_FEW
            (value in 3..4) && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_FEW

            // Many (5..10)
            (value in 5..10) && unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_MANY
            (value in 5..10) && unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_MANY
            (value in 5..10) && unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_MANY
            (value in 5..10) && unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_MANY

            // Other
            unit == Period.Unit.DAY -> VariableLocalizationKey.NUM_DAY_OTHER
            unit == Period.Unit.WEEK -> VariableLocalizationKey.NUM_WEEK_OTHER
            unit == Period.Unit.MONTH -> VariableLocalizationKey.NUM_MONTH_OTHER
            unit == Period.Unit.YEAR -> VariableLocalizationKey.NUM_YEAR_OTHER

            else -> VariableLocalizationKey.NUM_DAY_OTHER
        }

    private val Period.roundedValueInDays: String
        get() = valueInDays.roundToInt().toString()

    private val Period.roundedValueInWeeks: String
        get() = valueInWeeks.roundToInt().toString()

    private val Period.roundedValueInMonths: String
        get() = valueInMonths.roundToInt().toString()

    private val Period.roundedValueInYears: String
        get() = valueInYears.roundToInt().toString()

    private fun PricingPhase.canDisplay(unit: Period.Unit): Boolean =
        unit.ordinal <= billingPeriod.unit.ordinal
}
