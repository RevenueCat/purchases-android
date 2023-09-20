package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedPeriod
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
        return rcPackage.product.title
    }

    fun periodName(rcPackage: Package): String {
        val stringId = when (rcPackage.packageType) {
            PackageType.LIFETIME -> R.string.lifetime
            PackageType.ANNUAL -> R.string.annual
            PackageType.SIX_MONTH -> R.string.semester
            PackageType.THREE_MONTH -> R.string.quarter
            PackageType.TWO_MONTH -> R.string.bimonthly
            PackageType.MONTHLY -> R.string.monthly
            PackageType.WEEKLY -> R.string.weekly
            PackageType.UNKNOWN, PackageType.CUSTOM -> null
        }
        return stringId?.let { applicationContext.getString(it) } ?: ""
    }

    fun subscriptionDuration(rcPackage: Package): String? {
        return "SUBS_DURATION"
    }

    fun introductoryOfferDuration(rcPackage: Package): String? {
        return "INT_OFFER_DURATION"
    }

    fun localizedPricePerPeriod(rcPackage: Package, locale: Locale): String {
        val localizedPrice = localizedPrice(rcPackage)
        return rcPackage.product.period?.let { period ->
            var formattedPeriod = period.localizedPeriod(locale)
            if (period.value == 1 && formattedPeriod.startsWith("1")) {
                formattedPeriod = formattedPeriod.substring(startIndex = 1).trim()
            }
            "$localizedPrice/$formattedPeriod"
        } ?: localizedPrice
    }

    fun localizedPriceAndPerMonth(rcPackage: Package): String {
        return "PRICE_AND_PER_MONTH"
    }
}
