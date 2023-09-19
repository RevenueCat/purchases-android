package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import java.util.Locale

@Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
internal class VariableDataProvider(
    private val applicationContext: ApplicationContext,
) {
    val applicationName: String
        get() = applicationContext.getApplicationName()

    fun localizedPrice(rcPackage: Package): String {
        return rcPackage.product.price.formatted
    }

    fun localizedPricePerMonth(rcPackage: Package, locale: Locale): String {
        return rcPackage.product.formattedPricePerMonth(locale) ?: ""
    }

    fun localizedIntroductoryOfferPrice(rcPackage: Package): String? {
        return "INTRO_OFFER_PRICE"
    }

    fun productName(rcPackage: Package): String {
        return "PRODUCT_NAME"
    }

    fun periodName(rcPackage: Package): String {
        return "PERIOD_NAME"
    }

    fun subscriptionDuration(rcPackage: Package): String? {
        return "SUBS_DURATION"
    }

    fun introductoryOfferDuration(rcPackage: Package): String? {
        return "INT_OFFER_DURATION"
    }

    fun localizedPricePerPeriod(rcPackage: Package): String {
        return "PRICE_PER_PERIOD"
    }

    fun localizedPriceAndPerMonth(rcPackage: Package): String {
        return "PRICE_AND_PER_MONTH"
    }
}
