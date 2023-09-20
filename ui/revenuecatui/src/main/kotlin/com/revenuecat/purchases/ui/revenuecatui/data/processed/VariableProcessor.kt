package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

internal object VariableProcessor {
    private val REGEX = Regex("\\{\\{\\s[a-zA-Z0-9_]+\\s\\}\\}")

    fun processVariables(
        variableDataProvider: VariableDataProvider,
        originalString: String,
        rcPackage: Package,
        locale: Locale,
    ): String {
        var resultString = originalString
        REGEX.findAll(originalString).toList().reversed().forEach { matchResult ->
            val variableString = matchResult.value
            val variableWithoutBraces = variableString.substring(2, variableString.length - 2).trim()
            val newValue = variableValue(variableDataProvider, variableWithoutBraces, rcPackage, locale)
            newValue?.let {
                resultString = resultString.replaceRange(matchResult.range, it)
            }
        }
        return resultString
    }

    private fun variableValue(
        variableDataProvider: VariableDataProvider,
        variableName: String,
        rcPackage: Package,
        locale: Locale,
    ): String? {
        return when (variableName) {
            VariableName.APP_NAME.identifier -> variableDataProvider.applicationName
            VariableName.PRICE.identifier -> variableDataProvider.localizedPrice(rcPackage)
            VariableName.PRICE_PER_PERIOD.identifier -> variableDataProvider.localizedPricePerPeriod(rcPackage)
            VariableName.TOTAL_PRICE_AND_PER_MONTH.identifier -> variableDataProvider.localizedPriceAndPerMonth(
                rcPackage,
            )
            VariableName.PRODUCT_NAME.identifier -> variableDataProvider.productName(rcPackage)
            VariableName.SUB_PERIOD.identifier -> variableDataProvider.periodName(rcPackage)
            VariableName.SUB_PRICE_PER_MONTH.identifier -> variableDataProvider.localizedPricePerMonth(
                rcPackage,
                locale,
            )
            VariableName.SUB_DURATION.identifier -> variableDataProvider.subscriptionDuration(rcPackage)
            VariableName.SUB_OFFER_DURATION.identifier -> variableDataProvider.introductoryOfferDuration(rcPackage)
            VariableName.SUB_OFFER_PRICE.identifier -> variableDataProvider.localizedIntroductoryOfferPrice(rcPackage)
            else -> {
                Logger.e("Unknown variable: $variableName")
                null
            }
        }
    }

    enum class VariableName(val identifier: String) {
        APP_NAME("app_name"),
        PRICE("price"),
        PRICE_PER_PERIOD("price_per_period"),
        TOTAL_PRICE_AND_PER_MONTH("total_price_and_per_month"),
        PRODUCT_NAME("product_name"),
        SUB_PERIOD("sub_period"),
        SUB_PRICE_PER_MONTH("sub_price_per_month"),
        SUB_DURATION("sub_duration"),
        SUB_OFFER_DURATION("sub_offer_duration"),
        SUB_OFFER_PRICE("sub_offer_price"),
    }
}
