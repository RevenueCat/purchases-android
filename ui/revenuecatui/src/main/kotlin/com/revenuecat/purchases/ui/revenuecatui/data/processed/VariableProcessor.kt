package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.content.Context
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.Locale

internal object VariableProcessor {
    private val REGEX = Regex("\\{\\{\\s[a-zA-Z0-9_]+\\s\\}\\}")

    fun processVariables(
        context: Context,
        originalString: String,
        rcPackage: Package,
        locale: Locale,
    ): String {
        var resultString = originalString
        REGEX.findAll(originalString).toList().reversed().forEach { matchResult ->
            val newValue = variableValue(context, matchResult, rcPackage, locale)
            newValue?.let {
                resultString = resultString.replaceRange(matchResult.range, it)
            }
        }
        return resultString
    }

    private fun variableValue(
        context: Context,
        matchResult: MatchResult,
        rcPackage: Package,
        locale: Locale,
    ): String? {
        val variableString = matchResult.value
        return when (val variableWithoutBraces = variableString.substring(2, variableString.length - 2).trim()) {
            "app_name" -> VariableDataProvider.applicationName(context)
            "price" -> VariableDataProvider.localizedPrice(rcPackage)
            "price_per_period" -> VariableDataProvider.localizedPricePerPeriod(rcPackage)
            "total_price_and_per_month" -> VariableDataProvider.localizedPriceAndPerMonth(rcPackage)
            "product_name" -> VariableDataProvider.productName(rcPackage)
            "sub_period" -> VariableDataProvider.periodName(rcPackage)
            "sub_price_per_month" -> VariableDataProvider.localizedPricePerMonth(rcPackage, locale)
            "sub_duration" -> VariableDataProvider.subscriptionDuration(rcPackage)
            "sub_offer_duration" -> VariableDataProvider.introductoryOfferDuration(rcPackage)
            "sub_offer_price" -> VariableDataProvider.localizedIntroductoryOfferPrice(rcPackage)
            else -> {
                Logger.e("Unknown variable: $variableWithoutBraces")
                null
            }
        }
    }
}
