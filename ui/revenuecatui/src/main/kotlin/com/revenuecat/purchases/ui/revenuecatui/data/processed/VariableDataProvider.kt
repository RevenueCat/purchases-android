package com.revenuecat.purchases.ui.revenuecatui.data.processed

import android.content.Context
import com.revenuecat.purchases.Package
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

@Suppress("UnusedParameter", "FunctionOnlyReturningConstant")
internal class VariableDataProvider(
    private val applicationContext: Context,
) {
    companion object {
        private const val MICRO_MULTIPLIER = 1000000.0
    }

    val applicationName: String
        get() = applicationContext.applicationInfo.loadLabel(applicationContext.packageManager).toString()

    fun localizedPrice(rcPackage: Package): String {
        return rcPackage.product.price.formatted
    }

    fun localizedPricePerMonth(rcPackage: Package, locale: Locale): String {
        val price = rcPackage.product.price.amountMicros / MICRO_MULTIPLIER
        val periodMonths = rcPackage.product.period?.valueInMonths ?: 1.0
        val currencyCode = rcPackage.product.price.currencyCode
        val numberFormat = NumberFormat.getCurrencyInstance(locale)
        numberFormat.currency = Currency.getInstance(currencyCode)
        return numberFormat.format(price / periodMonths)
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
