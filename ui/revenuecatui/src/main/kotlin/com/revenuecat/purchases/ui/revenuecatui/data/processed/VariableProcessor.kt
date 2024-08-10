package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

internal object VariableProcessor {
    private val REGEX = Regex("\\{\\{\\s[a-zA-Z0-9_]+\\s\\}\\}")

    /**
     * Information necessary for computing variables.
     */
    data class PackageContext(
        val discountRelativeToMostExpensivePerMonth: Double?,
        val showZeroDecimalPlacePrices: Boolean = false,
    )

    /**
     * Returns a set of invalid variables in the String
     */
    fun validateVariables(originalString: String): Set<String> {
        val errors = mutableListOf<String>()
        handleVariablesAndReplace(originalString) { variable ->
            VariableName.valueOfIdentifier(variable) ?: errors.add(variable)
            null
        }
        return errors.toSet()
    }

    fun processVariables(
        variableDataProvider: VariableDataProvider,
        context: PackageContext,
        originalString: String,
        rcPackage: Package,
        locale: Locale,
    ): String {
        val resultString = handleVariablesAndReplace(originalString) { variable ->
            variableValue(variableDataProvider, context, variable, rcPackage, locale)
        }
        return resultString
    }

    private fun handleVariablesAndReplace(
        string: String,
        executeAndReplaceWith: (String) -> String?,
    ): String {
        var resultString = string
        REGEX.findAll(string).toList().reversed().forEach { matchResult ->
            val variableString = matchResult.value
            val variableWithoutBraces = variableString.substring(2, variableString.length - 2).trim()
            val replacement = executeAndReplaceWith(variableWithoutBraces)
            replacement?.let {
                resultString = resultString.replaceRange(matchResult.range, it)
            }
        }
        return resultString
    }

    private fun variableValue(
        variableDataProvider: VariableDataProvider,
        context: PackageContext,
        variableNameString: String,
        rcPackage: Package,
        locale: Locale,
    ): String? {
        val variableName = VariableName.valueOfIdentifier(variableNameString)
        return if (variableName == null) {
            Logger.e("Unknown variable: $variableNameString")
            null
        } else {
            return processVariable(variableName, variableDataProvider, context, rcPackage, locale) ?: run {
                Logger.w(
                    "Could not process value for variable '$variableNameString' for " +
                        "package '${rcPackage.identifier}'. Please check that the product for that package matches " +
                        "the requirements for that variable. Defaulting to empty string.",
                )
                ""
            }
        }
    }

    @SuppressWarnings("CyclomaticComplexMethod")
    private fun processVariable(
        variableName: VariableName,
        variableDataProvider: VariableDataProvider,
        context: PackageContext,
        rcPackage: Package,
        locale: Locale,
    ): String? = when (variableName) {
        VariableName.APP_NAME -> variableDataProvider.applicationName
        VariableName.PRICE -> variableDataProvider.localizedPrice(rcPackage, locale, context.showZeroDecimalPlacePrices)
        VariableName.PRICE_PER_PERIOD -> variableDataProvider.localizedPricePerPeriod(rcPackage, locale, context.showZeroDecimalPlacePrices)
        VariableName.PRICE_PER_PERIOD_FULL -> variableDataProvider.localizedPricePerPeriodFull(rcPackage, locale, context.showZeroDecimalPlacePrices)
        VariableName.TOTAL_PRICE_AND_PER_MONTH -> variableDataProvider.localizedPriceAndPerMonth(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )
        VariableName.TOTAL_PRICE_AND_PER_MONTH_FULL -> variableDataProvider.localizedPriceAndPerMonthFull(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )

        VariableName.PRODUCT_NAME -> variableDataProvider.productName(rcPackage)
        VariableName.SUB_PERIOD -> variableDataProvider.periodName(rcPackage)
        VariableName.SUB_PERIOD_LENGTH -> variableDataProvider.periodLength(rcPackage, locale)
        VariableName.SUB_PERIOD_ABBREVIATED -> variableDataProvider.periodNameAbbreviation(rcPackage, locale)
        VariableName.SUB_PRICE_PER_WEEK -> variableDataProvider.localizedPricePerWeek(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )
        VariableName.SUB_PRICE_PER_MONTH -> variableDataProvider.localizedPricePerMonth(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )

        VariableName.SUB_DURATION -> variableDataProvider.subscriptionDuration(rcPackage, locale)
        VariableName.SUB_DURATION_IN_MONTHS -> variableDataProvider.subscriptionDurationInMonths(rcPackage, locale)
        VariableName.SUB_OFFER_DURATION -> variableDataProvider.firstIntroductoryOfferDuration(rcPackage, locale)
        VariableName.SUB_OFFER_DURATION_2 -> variableDataProvider.secondIntroductoryOfferDuration(rcPackage, locale)
        VariableName.SUB_OFFER_PRICE -> variableDataProvider.localizedFirstIntroductoryOfferPrice(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )
        VariableName.SUB_OFFER_PRICE_2 -> variableDataProvider.localizedSecondIntroductoryOfferPrice(
            rcPackage,
            locale,
            context.showZeroDecimalPlacePrices,
        )
        VariableName.SUB_RELATIVE_DISCOUNT -> variableDataProvider.localizedRelativeDiscount(
            context.discountRelativeToMostExpensivePerMonth,
        )
    }

    private enum class VariableName(val identifier: String) {
        APP_NAME("app_name"),
        PRICE("price"),
        PRICE_PER_PERIOD("price_per_period"),
        PRICE_PER_PERIOD_FULL("price_per_period_full"),
        TOTAL_PRICE_AND_PER_MONTH("total_price_and_per_month"),
        TOTAL_PRICE_AND_PER_MONTH_FULL("total_price_and_per_month_full"),
        PRODUCT_NAME("product_name"),
        SUB_PERIOD("sub_period"),
        SUB_PERIOD_LENGTH("sub_period_length"),
        SUB_PERIOD_ABBREVIATED("sub_period_abbreviated"),
        SUB_PRICE_PER_WEEK("sub_price_per_week"),
        SUB_PRICE_PER_MONTH("sub_price_per_month"),
        SUB_DURATION("sub_duration"),
        SUB_DURATION_IN_MONTHS("sub_duration_in_months"),
        SUB_OFFER_DURATION("sub_offer_duration"),
        SUB_OFFER_DURATION_2("sub_offer_duration_2"),
        SUB_OFFER_PRICE("sub_offer_price"),
        SUB_OFFER_PRICE_2("sub_offer_price_2"),
        SUB_RELATIVE_DISCOUNT("sub_relative_discount"),
        ;

        companion object {
            private val valueMap by lazy {
                values().associateBy { it.identifier }
            }

            fun valueOfIdentifier(identifier: String): VariableName? {
                return valueMap[identifier]
            }
        }
    }
}
