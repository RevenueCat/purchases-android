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
import kotlin.math.roundToInt

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

    fun localizedFirstIntroductoryOfferPrice(rcPackage: Package): String? {
        return getFirstIntroOfferToApply(rcPackage)?.price?.formatted
    }

    fun localizedSecondIntroductoryOfferPrice(rcPackage: Package): String? {
        return getSecondIntroOfferToApply(rcPackage)?.price?.formatted
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

    fun subscriptionDurationInMonths(rcPackage: Package, locale: Locale): String? {
        return rcPackage.product.period?.normalizedMonths()?.localizedPeriod(locale)
            ?: periodName(rcPackage)
    }

    fun firstIntroductoryOfferDuration(rcPackage: Package, locale: Locale): String? {
        return getFirstIntroOfferToApply(rcPackage)?.billingPeriod?.localizedPeriod(locale)
    }

    fun secondIntroductoryOfferDuration(rcPackage: Package, locale: Locale): String? {
        return getSecondIntroOfferToApply(rcPackage)?.billingPeriod?.localizedPeriod(locale)
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

    @SuppressWarnings("MagicNumber")
    fun localizedRelativeDiscount(discountRelativeToMostExpensivePerMonth: Double?): String? {
        return applicationContext.localizedDiscount(discountRelativeToMostExpensivePerMonth)
    }

    private fun getFirstIntroOfferToApply(rcPackage: Package): PricingPhase? {
        val option = rcPackage.product.defaultOption
        return option?.freePhase ?: option?.introPhase
    }

    private fun getSecondIntroOfferToApply(rcPackage: Package): PricingPhase? {
        val option = rcPackage.product.defaultOption
        if (option?.freePhase != null) {
            return option.introPhase
        }
        return null
    }
}

internal fun TemplateConfiguration.PackageInfo.localizedDiscount(
    applicationContext: ApplicationContext,
): String? {
    return applicationContext.localizedDiscount(discountRelativeToMostExpensivePerMonth)
}

@SuppressWarnings("MagicNumber")
private fun ApplicationContext.localizedDiscount(
    discountRelativeToMostExpensivePerMonth: Double?,
): String? {
    return (discountRelativeToMostExpensivePerMonth?.times(100.0))?.roundToInt()?.let {
        getString(R.string.package_discount, it)
    }
}

/**
 * @return an equivalent [Period] using [Period.Unit.MONTH] whenever possible.
 */
@SuppressWarnings("MagicNumber")
private fun Period.normalizedMonths(): Period {
    return if (unit == Period.Unit.YEAR) {
        val months = value * 12
        return Period(months, Period.Unit.MONTH, "P${months}M")
    } else {
        this
    }
}
