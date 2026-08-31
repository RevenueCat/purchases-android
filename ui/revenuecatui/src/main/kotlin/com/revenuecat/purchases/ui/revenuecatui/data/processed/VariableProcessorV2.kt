package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.UiConfig
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.paywalls.components.CountdownComponent
import com.revenuecat.purchases.paywalls.components.common.VariableLocalizationKey
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.components.countdown.CountdownTime
import com.revenuecat.purchases.ui.revenuecatui.data.processed.VariableProcessor.PackageContext
import com.revenuecat.purchases.ui.revenuecatui.extensions.localized
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
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
        PRODUCT_ABSOLUTE_DISCOUNT("product.absolute_discount"),
        PRODUCT_OFFER_RELATIVE_DISCOUNT("product.offer_relative_discount"),
        PRODUCT_OFFER_ABSOLUTE_DISCOUNT("product.offer_absolute_discount"),
        PRODUCT_RELATIVE_DISCOUNT_WITH_OFFER("product.relative_discount_with_offer"),
        PRODUCT_ABSOLUTE_DISCOUNT_WITH_OFFER("product.absolute_discount_with_offer"),
        PRODUCT_STORE_PRODUCT_NAME("product.store_product_name"),

        COUNT_DAYS_WITH_ZERO("count_days_with_zero"),
        COUNT_DAYS_WITHOUT_ZERO("count_days_without_zero"),
        COUNT_HOURS_WITH_ZERO("count_hours_with_zero"),
        COUNT_HOURS_WITHOUT_ZERO("count_hours_without_zero"),
        COUNT_MINUTES_WITH_ZERO("count_minutes_with_zero"),
        COUNT_MINUTES_WITHOUT_ZERO("count_minutes_without_zero"),
        COUNT_SECONDS_WITH_ZERO("count_seconds_with_zero"),
        COUNT_SECONDS_WITHOUT_ZERO("count_seconds_without_zero"),
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

    /**
     * Prefixes that identify custom variables in paywall text.
     * Supports both `{{ custom.key }}` and `{{ $custom.key }}` syntax.
     */
    private val customVariablePrefixes = listOf("custom.", "\$custom.")

    @Suppress("LongParameterList")
    fun processVariables(
        template: String,
        localizedVariableKeys: Map<VariableLocalizationKey, String> = emptyMap(),
        variableConfig: UiConfig.VariableConfig,
        variableDataProvider: VariableDataProvider? = null,
        packageContext: PackageContext? = null,
        rcPackage: Package? = null,
        subscriptionOption: SubscriptionOption? = null,
        currencyLocale: Locale = Locale.getDefault(),
        dateLocale: Locale,
        date: Date = Date(),
        countdownTime: CountdownTime? = null,
        countFrom: CountdownComponent.CountFrom = CountdownComponent.CountFrom.DAYS,
        customVariables: Map<String, CustomVariableValue> = emptyMap(),
        defaultCustomVariables: Map<String, CustomVariableValue> = emptyMap(),
    ): String = template.replaceVariablesWithValues { variable, functions ->
        getVariableValue(
            variableIdentifier = variable,
            functionIdentifiers = functions,
            localizedVariableKeys = localizedVariableKeys,
            variableConfig = variableConfig,
            variableDataProvider = variableDataProvider,
            packageContext = packageContext,
            rcPackage = rcPackage,
            subscriptionOption = subscriptionOption,
            currencyLocale = currencyLocale,
            dateLocale = dateLocale,
            date = date,
            countdownTime = countdownTime,
            countFrom = countFrom,
            customVariables = customVariables,
            defaultCustomVariables = defaultCustomVariables,
        )
    }

    private fun String.replaceVariablesWithValues(
        getValue: (variable: String, functions: List<String>) -> String,
    ): String = buildString {
        val template = this@replaceVariablesWithValues
        var lastIndex = 0

        regex.findAll(template).forEach { match ->
            // Append everything between the previous match and this match.
            append(template, lastIndex, match.range.first)

            val (variableString) = match.destructured
            val parts = variableString.split("|").map { it.trim() }

            val variable = parts[0]
            val functions = if (parts.size > 1) parts.drop(1) else emptyList()

            // Append the value of this variable.
            append(getValue(variable, functions))

            lastIndex = match.range.last + 1
        }

        // Append the remainder of the template.
        append(template, lastIndex, template.length)
    }

    @Suppress("LongParameterList")
    private fun getVariableValue(
        variableIdentifier: String,
        functionIdentifiers: List<String>,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        variableConfig: UiConfig.VariableConfig,
        variableDataProvider: VariableDataProvider?,
        packageContext: PackageContext?,
        rcPackage: Package?,
        subscriptionOption: SubscriptionOption?,
        currencyLocale: Locale,
        dateLocale: Locale,
        date: Date,
        countdownTime: CountdownTime?,
        countFrom: CountdownComponent.CountFrom,
        customVariables: Map<String, CustomVariableValue>,
        defaultCustomVariables: Map<String, CustomVariableValue>,
    ): String {
        val functions = functionIdentifiers.mapNotNull { findFunction(it, variableConfig.functionCompatibilityMap) }

        // Check if this is a custom variable
        val customVariableKey = extractCustomVariableKey(variableIdentifier)
        if (customVariableKey != null) {
            return resolveCustomVariable(
                key = customVariableKey,
                customVariables = customVariables,
                defaultCustomVariables = defaultCustomVariables,
                functions = functions,
                currencyLocale = currencyLocale,
            )
        }

        val variable = findVariable(variableIdentifier, variableConfig.variableCompatibilityMap)
        return if (variable == null) {
            ""
        } else {
            val result = variable.getValue(
                localizedVariableKeys = localizedVariableKeys,
                variableDataProvider = variableDataProvider,
                packageContext = packageContext,
                rcPackage = rcPackage,
                subscriptionOption = subscriptionOption,
                currencyLocale = currencyLocale,
                dateLocale = dateLocale,
                date = date,
                countdownTime = countdownTime,
                countFrom = countFrom,
            )?.let { processedVariable ->
                functions.fold(processedVariable) { accumulator, function ->
                    accumulator.processFunction(function, currencyLocale)
                }
            }

            if (result != null) {
                result
            } else {
                if (rcPackage != null) {
                    Logger.failedToGetValue(variableIdentifier, rcPackage)
                }
                ""
            }
        }
    }

    /**
     * Extracts the custom variable key from a variable identifier.
     * Returns null if the identifier is not a custom variable.
     *
     * Examples:
     * - "custom.name" -> "name"
     * - "$custom.name" -> "name"
     * - "product.price" -> null
     */
    @Suppress("ReturnCount")
    private fun extractCustomVariableKey(variableIdentifier: String): String? {
        for (prefix in customVariablePrefixes) {
            if (variableIdentifier.startsWith(prefix)) {
                val key = variableIdentifier.removePrefix(prefix)
                if (key.isEmpty()) {
                    Logger.w(
                        "Custom variable '$variableIdentifier' appears to be malformed. " +
                            "Expected format: 'custom.<variable_name>' or '\$custom.<variable_name>'.",
                    )
                    return null
                }
                return key
            }
        }

        // Check for potential malformed custom variables
        checkForMalformedCustomVariable(variableIdentifier)

        return null
    }

    /**
     * Logs a warning if a variable identifier looks like it might be intended as a custom variable
     * but is malformed.
     */
    private fun checkForMalformedCustomVariable(variableIdentifier: String) {
        val malformedPrefixes = listOf("custom", "\$custom")
        for (prefix in malformedPrefixes) {
            if (variableIdentifier == prefix || variableIdentifier.startsWith("$prefix ")) {
                Logger.w(
                    "Variable '$variableIdentifier' looks like it might be intended as a custom variable. " +
                        "Use 'custom.<variable_name>' or '\$custom.<variable_name>' syntax instead.",
                )
                return
            }
        }
    }

    /**
     * Resolves a custom variable value using the following priority:
     * 1. SDK-provided value (from customVariables)
     * 2. Dashboard default value (from defaultCustomVariables)
     * 3. Empty string with a warning log
     *
     * Values are converted to their String representation during processing.
     */
    private fun resolveCustomVariable(
        key: String,
        customVariables: Map<String, CustomVariableValue>,
        defaultCustomVariables: Map<String, CustomVariableValue>,
        functions: List<Function>,
        currencyLocale: Locale,
    ): String {
        val value = customVariables[key]
            ?: defaultCustomVariables[key]
            ?: run {
                Logger.w(
                    "Custom variable '$key' was not provided and has no default value. Defaulting to empty string.",
                )
                return ""
            }

        return functions.fold(value.stringValue) { accumulator, function ->
            accumulator.processFunction(function, currencyLocale)
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
                        Logger.usingFallbackVariable(original = variableIdentifier, fallback = compatVariableIdentifier)
                    }
            } else {
                Logger.unsupportedVariableWithoutFallback(variableIdentifier)
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
                        Logger.usingFallbackFunction(original = functionIdentifier, fallback = compatFunctionIdentifier)
                    }
            } else {
                Logger.unsupportedFunctionWithoutFallback(functionIdentifier)
                null
            }
        }
    }

    private fun Logger.failedToGetValue(variableIdentifier: String, rcPackage: Package): Unit = w(
        "Could not process value for variable '$variableIdentifier' for " +
            "package '${rcPackage.identifier}'. Please check that the product for that package " +
            "matches the requirements for that variable. Defaulting to empty string.",
    )

    private fun Logger.usingFallbackVariable(original: String, fallback: String): Unit = w(
        "Paywall variable '$original' is not supported. Using backwards compatible " +
            "'$fallback' instead.",
    )

    private fun Logger.unsupportedVariableWithoutFallback(variableIdentifier: String): Unit = e(
        "Paywall variable '$variableIdentifier' is not supported and no backwards compatible " +
            "replacement found.",
    )

    private fun Logger.usingFallbackFunction(original: String, fallback: String): Unit = w(
        "Paywall function '$original' is not supported. Using backward compatible " +
            "'$fallback' instead.",
    )

    private fun Logger.unsupportedFunctionWithoutFallback(functionIdentifier: String): Unit =
        e(
            "Paywall function '$functionIdentifier' is not supported and no backwards compatible " +
                "replacement found.",
        )

    @Suppress("LongMethod", "CyclomaticComplexMethod", "LongParameterList", "NestedBlockDepth")
    private fun Variable.getValue(
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
        variableDataProvider: VariableDataProvider?,
        packageContext: PackageContext?,
        rcPackage: Package?,
        subscriptionOption: SubscriptionOption?,
        currencyLocale: Locale,
        dateLocale: Locale,
        date: Date,
        countdownTime: CountdownTime?,
        countFrom: CountdownComponent.CountFrom,
    ): String? = when (this) {
        Variable.PRODUCT_CURRENCY_CODE -> rcPackage?.product?.price?.currencyCode
        Variable.PRODUCT_CURRENCY_SYMBOL -> rcPackage?.let {
            Currency.getInstance(it.product.price.currencyCode).getSymbol(currencyLocale)
        }

        Variable.PRODUCT_PERIODLY -> rcPackage?.productPeriodly(localizedVariableKeys)

        Variable.PRODUCT_PRICE -> rcPackage?.let { pkg ->
            variableDataProvider?.localizedPrice(
                rcPackage = pkg,
                locale = currencyLocale,
                showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false,
            )
        }

        Variable.PRODUCT_PRICE_PER_PERIOD -> rcPackage?.let { pkg ->
            variableDataProvider?.localizedPrice(
                rcPackage = pkg,
                locale = currencyLocale,
                showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false,
            )?.let { price ->
                val period = pkg.productPeriod(localizedVariableKeys)
                when {
                    pkg.hasNoBillingPeriod -> price
                    period != null -> "$price/$period"
                    else -> null
                }
            }
        }

        Variable.PRODUCT_PRICE_PER_PERIOD_ABBREVIATED -> rcPackage?.let { pkg ->
            variableDataProvider?.localizedPrice(
                rcPackage = pkg,
                locale = currencyLocale,
                showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false,
            )?.let { price ->
                val period = pkg.productPeriodAbbreviated(localizedVariableKeys)
                when {
                    pkg.hasNoBillingPeriod -> price
                    period != null -> "$price/$period"
                    else -> null
                }
            }
        }

        Variable.PRODUCT_PRICE_PER_DAY -> rcPackage?.let { pkg ->
            val showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false
            when {
                pkg.hasNoBillingPeriod ->
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZeroDecimalPlacePrices)
                else -> variableDataProvider?.localizedPricePerDay(pkg, currencyLocale, showZeroDecimalPlacePrices)
            }
        }

        Variable.PRODUCT_PRICE_PER_WEEK -> rcPackage?.let { pkg ->
            val showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false
            when {
                pkg.hasNoBillingPeriod ->
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZeroDecimalPlacePrices)
                else -> variableDataProvider?.localizedPricePerWeek(pkg, currencyLocale, showZeroDecimalPlacePrices)
            }
        }

        Variable.PRODUCT_PRICE_PER_MONTH -> rcPackage?.let { pkg ->
            val showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false
            when {
                pkg.hasNoBillingPeriod ->
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZeroDecimalPlacePrices)
                else -> variableDataProvider?.localizedPricePerMonth(pkg, currencyLocale, showZeroDecimalPlacePrices)
            }
        }

        Variable.PRODUCT_PRICE_PER_YEAR -> rcPackage?.let { pkg ->
            val showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false
            when {
                pkg.hasNoBillingPeriod ->
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZeroDecimalPlacePrices)
                else -> variableDataProvider?.localizedPricePerYear(pkg, currencyLocale, showZeroDecimalPlacePrices)
            }
        }

        Variable.PRODUCT_PERIOD -> rcPackage?.productPeriod(localizedVariableKeys)
        Variable.PRODUCT_PERIOD_ABBREVIATED -> rcPackage?.productPeriodAbbreviated(localizedVariableKeys)
        Variable.PRODUCT_PERIOD_IN_DAYS -> rcPackage?.product?.period?.roundedValueInDays
        Variable.PRODUCT_PERIOD_IN_WEEKS -> rcPackage?.product?.period?.roundedValueInWeeks
        Variable.PRODUCT_PERIOD_IN_MONTHS -> rcPackage?.product?.period?.roundedValueInMonths
        Variable.PRODUCT_PERIOD_IN_YEARS -> rcPackage?.product?.period?.roundedValueInYears
        Variable.PRODUCT_PERIOD_WITH_UNIT -> rcPackage?.productPeriodWithUnit(localizedVariableKeys)
        Variable.PRODUCT_OFFER_PRICE ->
            resolveOfferPrice(
                discountPhase = primaryDiscountPhase(subscriptionOption, rcPackage),
                locale = currencyLocale,
                localizedVariableKeys = localizedVariableKeys,
            ) {
                rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZero)
                }
            }

        Variable.PRODUCT_OFFER_PRICE_PER_DAY ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.productOfferPricePerDay(currencyLocale, localizedVariableKeys)
                ?: rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPricePerDay(pkg, currencyLocale, showZero)
                }

        Variable.PRODUCT_OFFER_PRICE_PER_WEEK ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.productOfferPricePerWeek(currencyLocale, localizedVariableKeys)
                ?: rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPricePerWeek(pkg, currencyLocale, showZero)
                }

        Variable.PRODUCT_OFFER_PRICE_PER_MONTH ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.productOfferPricePerMonth(currencyLocale, localizedVariableKeys)
                ?: rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPricePerMonth(pkg, currencyLocale, showZero)
                }

        Variable.PRODUCT_OFFER_PRICE_PER_YEAR ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.productOfferPricePerYear(currencyLocale, localizedVariableKeys)
                ?: rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPricePerYear(pkg, currencyLocale, showZero)
                }

        Variable.PRODUCT_OFFER_PERIOD ->
            resolveOfferPeriod(
                discountPhase = primaryDiscountPhase(subscriptionOption, rcPackage),
                discountValue = { it.productOfferPeriod(localizedVariableKeys) },
            ) { rcPackage?.productPeriod(localizedVariableKeys) }

        Variable.PRODUCT_OFFER_PERIOD_ABBREVIATED ->
            resolveOfferPeriod(
                discountPhase = primaryDiscountPhase(subscriptionOption, rcPackage),
                discountValue = { it.productOfferPeriodAbbreviated(localizedVariableKeys) },
            ) { rcPackage?.productPeriodAbbreviated(localizedVariableKeys) }

        Variable.PRODUCT_OFFER_PERIOD_IN_DAYS ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferPeriodInDays
                ?: rcPackage?.product?.period?.roundedValueInDays

        Variable.PRODUCT_OFFER_PERIOD_IN_WEEKS ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferPeriodInWeeks
                ?: rcPackage?.product?.period?.roundedValueInWeeks

        Variable.PRODUCT_OFFER_PERIOD_IN_MONTHS ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferPeriodInMonths
                ?: rcPackage?.product?.period?.roundedValueInMonths

        Variable.PRODUCT_OFFER_PERIOD_IN_YEARS ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferPeriodInYears
                ?: rcPackage?.product?.period?.roundedValueInYears

        Variable.PRODUCT_OFFER_PERIOD_WITH_UNIT ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferPeriodWithUnit(localizedVariableKeys)
                ?: rcPackage?.productPeriodWithUnit(localizedVariableKeys)

        Variable.PRODUCT_OFFER_END_DATE ->
            primaryDiscountPhase(subscriptionOption, rcPackage)?.productOfferEndDate(dateLocale, date)
        Variable.PRODUCT_SECONDARY_OFFER_PRICE ->
            resolveOfferPrice(
                discountPhase = secondaryDiscountPhase(subscriptionOption, rcPackage),
                locale = currencyLocale,
                localizedVariableKeys = localizedVariableKeys,
            ) {
                rcPackage?.let { pkg ->
                    val showZero = packageContext?.showZeroDecimalPlacePrices ?: false
                    variableDataProvider?.localizedPrice(pkg, currencyLocale, showZero)
                }
            }

        Variable.PRODUCT_SECONDARY_OFFER_PERIOD ->
            resolveOfferPeriod(
                discountPhase = secondaryDiscountPhase(subscriptionOption, rcPackage),
                discountValue = { it.productOfferPeriod(localizedVariableKeys) },
            ) { rcPackage?.productPeriod(localizedVariableKeys) }

        Variable.PRODUCT_SECONDARY_OFFER_PERIOD_ABBREVIATED ->
            resolveOfferPeriod(
                discountPhase = secondaryDiscountPhase(subscriptionOption, rcPackage),
                discountValue = { it.productOfferPeriodAbbreviated(localizedVariableKeys) },
            ) { rcPackage?.productPeriodAbbreviated(localizedVariableKeys) }

        Variable.PRODUCT_RELATIVE_DISCOUNT -> packageContext?.relativeDiscount(localizedVariableKeys)

        Variable.PRODUCT_ABSOLUTE_DISCOUNT -> packageContext?.absoluteDiscount(rcPackage, currencyLocale)

        Variable.PRODUCT_OFFER_RELATIVE_DISCOUNT ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.comparableOfferPriceMicros(rcPackage)
                ?.let { offerPriceMicros ->
                    offerRelativeDiscount(rcPackage, offerPriceMicros, localizedVariableKeys)
                }

        Variable.PRODUCT_OFFER_ABSOLUTE_DISCOUNT ->
            primaryDiscountPhase(subscriptionOption, rcPackage)
                ?.comparableOfferPriceMicros(rcPackage)
                ?.let { offerPriceMicros ->
                    offerAbsoluteDiscount(
                        rcPackage = rcPackage,
                        offerPriceMicros = offerPriceMicros,
                        locale = currencyLocale,
                        showZeroDecimalPlacePrices = packageContext?.showZeroDecimalPlacePrices ?: false,
                    )
                }

        Variable.PRODUCT_RELATIVE_DISCOUNT_WITH_OFFER ->
            packageContext?.relativeDiscountWithOffer(rcPackage, subscriptionOption, localizedVariableKeys)

        Variable.PRODUCT_ABSOLUTE_DISCOUNT_WITH_OFFER ->
            packageContext?.absoluteDiscountWithOffer(rcPackage, subscriptionOption, currencyLocale)

        Variable.PRODUCT_STORE_PRODUCT_NAME -> rcPackage?.product?.name

        Variable.COUNT_DAYS_WITH_ZERO -> countdownTime?.let {
            val days = when (countFrom) {
                CountdownComponent.CountFrom.DAYS -> it.days
                CountdownComponent.CountFrom.HOURS,
                CountdownComponent.CountFrom.MINUTES,
                -> 0
            }
            String.format(dateLocale, "%02d", days)
        } ?: ""

        Variable.COUNT_DAYS_WITHOUT_ZERO -> countdownTime?.let {
            val days = when (countFrom) {
                CountdownComponent.CountFrom.DAYS -> it.days
                CountdownComponent.CountFrom.HOURS,
                CountdownComponent.CountFrom.MINUTES,
                -> 0
            }
            String.format(dateLocale, "%d", days)
        } ?: ""

        Variable.COUNT_HOURS_WITH_ZERO -> countdownTime?.let {
            val hours = when (countFrom) {
                CountdownComponent.CountFrom.DAYS -> it.hours
                CountdownComponent.CountFrom.HOURS -> it.totalHours
                CountdownComponent.CountFrom.MINUTES -> 0
            }
            String.format(dateLocale, "%02d", hours)
        } ?: ""

        Variable.COUNT_HOURS_WITHOUT_ZERO -> countdownTime?.let {
            val hours = when (countFrom) {
                CountdownComponent.CountFrom.DAYS -> it.hours
                CountdownComponent.CountFrom.HOURS -> it.totalHours
                CountdownComponent.CountFrom.MINUTES -> 0
            }
            String.format(dateLocale, "%d", hours)
        } ?: ""

        Variable.COUNT_MINUTES_WITH_ZERO -> countdownTime?.let {
            val minutes = when (countFrom) {
                CountdownComponent.CountFrom.DAYS,
                CountdownComponent.CountFrom.HOURS,
                -> it.minutes

                CountdownComponent.CountFrom.MINUTES -> it.totalMinutes
            }
            String.format(dateLocale, "%02d", minutes)
        } ?: ""

        Variable.COUNT_MINUTES_WITHOUT_ZERO -> countdownTime?.let {
            val minutes = when (countFrom) {
                CountdownComponent.CountFrom.DAYS,
                CountdownComponent.CountFrom.HOURS,
                -> it.minutes

                CountdownComponent.CountFrom.MINUTES -> it.totalMinutes
            }
            String.format(dateLocale, "%d", minutes)
        } ?: ""

        Variable.COUNT_SECONDS_WITH_ZERO -> countdownTime?.seconds?.let { String.format(dateLocale, "%02d", it) } ?: ""
        Variable.COUNT_SECONDS_WITHOUT_ZERO -> countdownTime?.seconds?.let { String.format(dateLocale, "%d", it) } ?: ""
    }

    private fun String.processFunction(function: Function, locale: Locale): String = when (function) {
        Function.LOWERCASE -> lowercase()
        Function.UPPERCASE -> uppercase()
        // This is the recommended replacement for capitalize().
        Function.CAPITALIZE -> replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }

    private fun PackageContext.relativeDiscount(localizedVariableKeys: Map<VariableLocalizationKey, String>): String? =
        discountRelativeToMostExpensivePerMonth
            ?.let { discount -> (discount * PERCENT_SCALE).roundToInt() }
            ?.let { discountPercentage ->
                localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.PERCENT)?.format(discountPercentage)
            }

    /**
     * The saving against the most expensive package, expressed over this package's own period.
     *
     * The anchor is picked by per-month price, exactly as [relativeDiscount] does, so the two variables
     * always compare the same pair of packages. The amount is then normalized to the period being
     * purchased: a ratio is period-invariant, but a currency amount is not, so quoting it per month would
     * peg the number to an arbitrary unit rather than to what the customer actually buys.
     */
    private fun PackageContext.absoluteDiscount(rcPackage: Package?, locale: Locale): String? {
        val product = rcPackage?.product ?: return null
        val mostExpensiveMicros = mostExpensivePricePerMonthMicros ?: return null
        val pricePerMonthMicros = product.pricePerMonth(locale)?.amountMicros ?: return null
        // Non-subscriptions have no period to normalize the anchor to.
        val periodInMonths = product.period?.valueInMonths ?: return null

        if (mostExpensiveMicros <= pricePerMonthMicros) return null

        val savingMicros = (mostExpensiveMicros * periodInMonths).toLong() - product.price.amountMicros
        if (savingMicros <= 0L) return null

        // `formatted` is unused: localized() derives the display string from amountMicros.
        return Price("", savingMicros, product.price.currencyCode).localized(locale, showZeroDecimalPlacePrices)
    }

    /**
     * What the customer actually pays and over how long: the primary offer's, or the package's own
     * when there is no usable offer. A free offer counts as no offer — "100% off" isn't the claim
     * these variables make.
     *
     * [totalMicros] and [months] are null only when the phase repeats until cancellation, because there
     * is then no term over which a total saving accrues. [ratePerMonthMicros] is still known in that
     * case, so the relative variant keeps working.
     */
    private data class EffectiveTerm(
        val ratePerMonthMicros: Double,
        val totalMicros: Long?,
        val months: Double?,
    )

    private fun effectiveTerm(rcPackage: Package?, subscriptionOption: SubscriptionOption?): EffectiveTerm? {
        val product = rcPackage?.product ?: return null
        val phase = primaryDiscountPhase(subscriptionOption, rcPackage)

        if (phase != null && phase.price.amountMicros > 0L) {
            val periodInMonths = phase.billingPeriod.valueInMonths
            if (periodInMonths <= 0.0) return null
            val cycles = phase.offerCycleCount()

            return EffectiveTerm(
                ratePerMonthMicros = phase.price.amountMicros / periodInMonths,
                totalMicros = cycles?.let { phase.price.amountMicros * it },
                months = cycles?.let { periodInMonths * it },
            )
        }

        // Non-subscriptions have no period to normalize the anchor to.
        val periodInMonths = product.period?.valueInMonths ?: return null
        if (periodInMonths <= 0.0) return null

        return EffectiveTerm(
            ratePerMonthMicros = product.price.amountMicros / periodInMonths,
            totalMicros = product.price.amountMicros,
            months = periodInMonths,
        )
    }

    /**
     * How many times this phase is billed, or null when it repeats until cancellation.
     *
     * Deliberately keys off [RecurrenceMode] rather than `billingCycleCount` being null. Play's
     * `getBillingCycleCount()` is a primitive int, so `toRevenueCatPricingPhase` passes through 0 —
     * never null — for non-finite phases, despite what the property's doc comment says. Treating 0 as
     * "no cycles" would multiply a single-payment offer's total out to zero and silently blank it.
     */
    private fun PricingPhase.offerCycleCount(): Int? = when (recurrenceMode) {
        RecurrenceMode.INFINITE_RECURRING -> null
        RecurrenceMode.NON_RECURRING -> 1
        RecurrenceMode.FINITE_RECURRING,
        RecurrenceMode.UNKNOWN,
        -> billingCycleCount?.takeIf { it > 0 } ?: 1
    }

    private fun PackageContext.relativeDiscountWithOffer(
        rcPackage: Package?,
        subscriptionOption: SubscriptionOption?,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? {
        val anchorMicros = mostExpensivePricePerMonthMicros ?: return null
        val term = effectiveTerm(rcPackage, subscriptionOption) ?: return null
        if (term.ratePerMonthMicros >= anchorMicros) return null

        val percentage = ((anchorMicros - term.ratePerMonthMicros) * PERCENT_SCALE / anchorMicros).roundToInt()
        return localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.PERCENT)?.format(percentage)
    }

    private fun PackageContext.absoluteDiscountWithOffer(
        rcPackage: Package?,
        subscriptionOption: SubscriptionOption?,
        locale: Locale,
    ): String? {
        val anchorMicros = mostExpensivePricePerMonthMicros ?: return null
        val currencyCode = rcPackage?.product?.price?.currencyCode ?: return null
        val term = effectiveTerm(rcPackage, subscriptionOption) ?: return null
        val totalMicros = term.totalMicros ?: return null
        val months = term.months ?: return null

        // Compare totals rather than scaling a monthly rate back up, so no rounding is amplified.
        val savingMicros = (anchorMicros * months).toLong() - totalMicros
        if (savingMicros <= 0L) return null

        // `formatted` is unused: localized() derives the display string from amountMicros.
        return Price("", savingMicros, currencyCode).localized(locale, showZeroDecimalPlacePrices)
    }

    private fun offerRelativeDiscount(
        rcPackage: Package?,
        offerPriceMicros: Long,
        localizedVariableKeys: Map<VariableLocalizationKey, String>,
    ): String? {
        val standardMicros = rcPackage?.product?.price?.amountMicros ?: return null
        if (standardMicros <= offerPriceMicros) return null

        val percentage =
            ((standardMicros - offerPriceMicros).toDouble() * PERCENT_SCALE / standardMicros).roundToInt()
        return localizedVariableKeys.getStringOrLogError(VariableLocalizationKey.PERCENT)?.format(percentage)
    }

    private fun offerAbsoluteDiscount(
        rcPackage: Package?,
        offerPriceMicros: Long,
        locale: Locale,
        showZeroDecimalPlacePrices: Boolean,
    ): String? {
        val standardPrice = rcPackage?.product?.price ?: return null
        val savingMicros = standardPrice.amountMicros - offerPriceMicros
        if (savingMicros <= 0L) return null

        // `formatted` is unused: localized() derives the display string from amountMicros.
        return Price("", savingMicros, standardPrice.currencyCode).localized(locale, showZeroDecimalPlacePrices)
    }
}
