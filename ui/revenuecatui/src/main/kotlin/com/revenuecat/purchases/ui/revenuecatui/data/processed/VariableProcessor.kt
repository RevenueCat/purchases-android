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
            "app_name" -> variableDataProvider.applicationName
            "price" -> variableDataProvider.localizedPrice(rcPackage)
            "price_per_period" -> variableDataProvider.localizedPricePerPeriod(rcPackage, locale)
            "total_price_and_per_month" -> variableDataProvider.localizedPriceAndPerMonth(rcPackage)
            "product_name" -> variableDataProvider.productName(rcPackage)
            "sub_period" -> variableDataProvider.periodName(rcPackage)
            "sub_price_per_month" -> variableDataProvider.localizedPricePerMonth(rcPackage, locale)
            "sub_duration" -> variableDataProvider.subscriptionDuration(rcPackage, locale)
            "sub_offer_duration" -> variableDataProvider.introductoryOfferDuration(rcPackage)
            "sub_offer_price" -> variableDataProvider.localizedIntroductoryOfferPrice(rcPackage)
            else -> {
                Logger.e("Unknown variable: $variableName")
                null
            }
        }
    }
}
