package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.extensions.isMonthly
import com.revenuecat.purchases.ui.revenuecatui.extensions.isSubscription
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedAbbreviatedPeriod
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedPeriod
import com.revenuecat.purchases.ui.revenuecatui.helpers.ApplicationContext
import java.util.Locale

@Suppress("UnusedParameter", "FunctionOnlyReturningConstant", "TooManyFunctions")
internal class VariableDataProvider(
    private val applicationContext: ApplicationContext,
    private val preview: Boolean = false,
) {
    val applicationName: String
        get() = if (!preview) {
            applicationContext.getApplicationName()
        } else {
            "Application Name"
        }

    fun localizedPrice(rcPackage: Package): String {
        return rcPackage.product.price.formatted
    }

    fun localizedPricePerMonth(rcPackage: Package, locale: Locale): String? {
        return rcPackage.product.formattedPricePerMonth(locale)
    }

    fun localizedFreeOrIntroductoryOfferPrice(rcPackage: Package): String? {
        return getFreeOrIntroPhaseToApply(rcPackage)?.price?.formatted
    }

    fun localizedIntroductoryOfferPrice(rcPackage: Package): String? {
        return getIntroPhaseToApply(rcPackage)?.price?.formatted
    }

    fun productName(rcPackage: Package): String {
        return rcPackage.product.title
    }

    fun periodName(rcPackage: Package): String? {
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
        return stringId?.let { applicationContext.getString(it) }
    }

    fun subscriptionDuration(rcPackage: Package, locale: Locale): String? {
        return rcPackage.product.period?.localizedPeriod(locale)
            ?: periodName(rcPackage)
    }

    fun introductoryOfferDuration(rcPackage: Package, locale: Locale): String? {
        return getFreeOrIntroPhaseToApply(rcPackage)?.billingPeriod?.localizedPeriod(locale)
    }

    fun localizedPricePerPeriod(rcPackage: Package, locale: Locale): String {
        val localizedPrice = localizedPrice(rcPackage)
        return rcPackage.product.period?.let { period ->
            val formattedPeriod = period.localizedAbbreviatedPeriod(locale)
            "$localizedPrice/$formattedPeriod"
        } ?: localizedPrice
    }

    fun localizedPriceAndPerMonth(rcPackage: Package, locale: Locale): String {
        if (!rcPackage.isSubscription || rcPackage.isMonthly) {
            return localizedPricePerPeriod(rcPackage, locale)
        }
        val unit = Period(1, Period.Unit.MONTH, "P1M").localizedAbbreviatedPeriod(locale)
        val pricePerPeriod = localizedPricePerPeriod(rcPackage, locale)
        val pricePerMonth = localizedPricePerMonth(rcPackage, locale)
        return "$pricePerPeriod ($pricePerMonth/$unit)"
    }

    private fun getFreeOrIntroPhaseToApply(rcPackage: Package): PricingPhase? {
        val option = rcPackage.product.defaultOption
        return option?.freePhase ?: option?.introPhase
    }

    private fun getIntroPhaseToApply(rcPackage: Package): PricingPhase? {
        val option = rcPackage.product.defaultOption
        return option?.introPhase
    }
}
