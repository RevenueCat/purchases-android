package com.revenuecat.purchases.ui.revenuecatui.data.processed

import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.ui.revenuecatui.R
import com.revenuecat.purchases.ui.revenuecatui.extensions.isMonthly
import com.revenuecat.purchases.ui.revenuecatui.extensions.isSubscription
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedAbbreviatedPeriod
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedPeriod
import com.revenuecat.purchases.ui.revenuecatui.extensions.localizedUnitPeriod
import com.revenuecat.purchases.ui.revenuecatui.helpers.ResourceProvider
import java.text.NumberFormat
import java.text.ParseException
import java.util.Currency
import java.util.Locale
import kotlin.math.roundToInt

@Suppress("UnusedParameter", "FunctionOnlyReturningConstant", "TooManyFunctions")
internal class VariableDataProvider(
    private val resourceProvider: ResourceProvider,
    private val preview: Boolean = false,
) {
    val applicationName: String
        get() = if (!preview) {
            resourceProvider.getApplicationName()
        } else {
            "Application Name"
        }

    private fun paywallLocale(deviceLocal: Locale): Locale {
        val storeCountryCode = Purchases.sharedInstance.storefrontCountryCode
        if( storeCountryCode == null ) {
            return deviceLocal
        }
        val storeLocale = Locale(deviceLocal.language, storeCountryCode)
        return storeLocale
    }

    private fun priceEndsIn99or00Cents(priceString: String, locale: Locale, currencyCode: String): Boolean {
        val storeCountryCode = Purchases.sharedInstance.storefrontCountryCode
        if( storeCountryCode == null ) {
            return false
        }

        val storeLocale = Locale(locale.language, storeCountryCode)
        val formatter = NumberFormat.getCurrencyInstance(storeLocale)
        formatter.currency = Currency.getInstance(currencyCode)
        return try {
            val price = formatter.parse(priceString)?.toDouble()

            if (price == null) {
                return false
            }

            val roundedCents = (price * 100).toInt() % 100
            return roundedCents == 99 || roundedCents == 0
        } catch (e: ParseException) {
            false
        }
    }

    private fun priceEndsIn99or00Cents2(priceMicros: Long): Boolean {
        val normalPrice = priceMicros.toDouble() / 1_000_000
        val roundedCents = (normalPrice * 100).toInt() % 100
        return roundedCents == 99 || roundedCents == 0
    }

    private fun roundCurrencyPrice2(priceMicros: Long, deviceLocale: Locale, currencyCode: String): String {
        val storeCountryCode = Purchases.sharedInstance.storefrontCountryCode
        if( storeCountryCode == null ) {
            return "ERROR!"
        }
        val storeLocale = Locale(deviceLocale.language, storeCountryCode)
        val currencyFormat = NumberFormat.getCurrencyInstance(storeLocale)

        currencyFormat.maximumFractionDigits = 0
        currencyFormat.currency = Currency.getInstance(currencyCode)

        return try {
            val price = priceMicros.toDouble() / 1_000_000
            val roundedNumber = Math.round(price.toDouble())
            val roundedString = currencyFormat.format(roundedNumber)

            return roundedString
        } catch (e: ParseException) {
            return "ERROR 2!"
        }
    }

    private fun roundCurrencyPrice(priceString: String, locale: Locale, currencyCode: String): String {

//        val currencyFormat = NumberFormat.getCurrencyInstance(locale)
        val storeCountryCode = Purchases.sharedInstance.storefrontCountryCode
        if( storeCountryCode == null ) {
            return priceString
        }
        val storeLocale = Locale(locale.language, storeCountryCode)
        val currencyFormat = NumberFormat.getCurrencyInstance(storeLocale)

        currencyFormat.maximumFractionDigits = 0
        currencyFormat.currency = Currency.getInstance(currencyCode)

        return try {
            val parsedNumber = currencyFormat.parse(priceString) ?: return priceString
            val roundedNumber = Math.round(parsedNumber.toDouble())
            val roundedString = currencyFormat.format(roundedNumber)

            return roundedString
        } catch (e: ParseException) {
            priceString
        }
    }

    fun localizedPrice(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
        // always round if rounding on
        return if (showZeroDecimalPlacePrices) {
            val currencyCode = rcPackage.product.price.currencyCode
//            roundCurrencyPrice(rcPackage.product.price.formatted, locale, currencyCode)
              roundCurrencyPrice2(rcPackage.product.price.amountMicros, locale, currencyCode)
        } else {
            rcPackage.product.price.formatted
        }
    }

    fun localizedPricePerWeek(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String? {
        // round if rounding on and price ends in 99 or 00
//        val priceStringOld = rcPackage.product.pricePerWeek(locale)?.formatted ?: return null
        val pricePerWeek = rcPackage.product.pricePerWeek(paywallLocale(locale)) ?: return null

        val currencyCode = rcPackage.product.price.currencyCode
        return if (showZeroDecimalPlacePrices && priceEndsIn99or00Cents2(pricePerWeek.amountMicros) /*priceEndsIn99or00Cents(priceString, locale, currencyCode)*/) {
//            roundCurrencyPrice(priceString, locale, currencyCode)
            roundCurrencyPrice2(rcPackage.product.price.amountMicros, locale, currencyCode)
        } else {
            pricePerWeek.formatted
        }
    }

    fun localizedPricePerMonth(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String? {
        // round if rounding on and price ends in 99 or 00
//        val priceStringOld = rcPackage.product.pricePerMonth(locale)?.formatted ?: return null
        val pricePerMonth = rcPackage.product.pricePerMonth(paywallLocale(locale)) ?: return null
        val currencyCode = rcPackage.product.price.currencyCode
        return if (showZeroDecimalPlacePrices && priceEndsIn99or00Cents2(pricePerMonth.amountMicros) /*priceEndsIn99or00Cents(priceString, locale, currencyCode)*/) {
//            roundCurrencyPrice(priceString, locale, currencyCode)
            roundCurrencyPrice2(rcPackage.product.price.amountMicros, locale, currencyCode)
        } else {
            pricePerMonth.formatted
        }
    }

    fun localizedFirstIntroductoryOfferPrice(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String? {
        // always round if rounding on
        val priceString = getFirstIntroOfferToApply(rcPackage)?.price?.formatted ?: return null

        return if (showZeroDecimalPlacePrices) {
            val currencyCode = rcPackage.product.price.currencyCode
//            roundCurrencyPrice(priceString, locale, currencyCode)
            roundCurrencyPrice2(rcPackage.product.price.amountMicros, locale, currencyCode)
        } else {
            priceString
        }
    }

    fun localizedSecondIntroductoryOfferPrice(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String? {
        // always round if rounding on
        val priceString = getSecondIntroOfferToApply(rcPackage)?.price?.formatted ?: return null

        return if (showZeroDecimalPlacePrices) {
            val currencyCode = rcPackage.product.price.currencyCode
//            roundCurrencyPrice(priceString, locale, currencyCode)
            roundCurrencyPrice2(rcPackage.product.price.amountMicros, locale, currencyCode)
        } else {
            priceString
        }
    }

    fun productName(rcPackage: Package): String {
        return rcPackage.product.name
    }

    fun periodName(rcPackage: Package): String? {
        if (rcPackage.packageType == PackageType.CUSTOM ||
            rcPackage.packageType == PackageType.UNKNOWN
        ) {
            return rcPackage.identifier
        }
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
        return stringId?.let { resourceProvider.getString(it) }
    }

    fun periodLength(rcPackage: Package, locale: Locale): String? {
        return rcPackage.product.period?.localizedUnitPeriod(locale)
    }

    fun periodNameAbbreviation(rcPackage: Package, locale: Locale): String? {
        return rcPackage.product.period?.localizedAbbreviatedPeriod(locale)
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

    fun localizedPricePerPeriod(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
        val localizedPrice = localizedPrice(rcPackage, locale, showZeroDecimalPlacePrices)
        return rcPackage.product.period?.let { period ->
            val formattedPeriod = period.localizedAbbreviatedPeriod(locale)
            "$localizedPrice/$formattedPeriod"
        } ?: localizedPrice
    }

    fun localizedPricePerPeriodFull(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
        val localizedPrice = localizedPrice(rcPackage, locale, showZeroDecimalPlacePrices)
        return rcPackage.product.period?.let { period ->
            val formattedPeriod = period.localizedUnitPeriod(locale)
            "$localizedPrice/$formattedPeriod"
        } ?: localizedPrice
    }

    fun localizedPriceAndPerMonth(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
        if (!rcPackage.isSubscription || rcPackage.isMonthly) {
            return localizedPricePerPeriod(rcPackage, locale, showZeroDecimalPlacePrices)
        }
        val unit = Period(1, Period.Unit.MONTH, "P1M").localizedAbbreviatedPeriod(locale)
        val pricePerPeriod = localizedPricePerPeriod(rcPackage, locale, showZeroDecimalPlacePrices)
        val pricePerMonth = localizedPricePerMonth(rcPackage, locale, showZeroDecimalPlacePrices)
        return "$pricePerPeriod ($pricePerMonth/$unit)"
    }

    fun localizedPriceAndPerMonthFull(rcPackage: Package, locale: Locale, showZeroDecimalPlacePrices: Boolean): String {
        if (!rcPackage.isSubscription || rcPackage.isMonthly) {
            return localizedPricePerPeriodFull(rcPackage, locale, showZeroDecimalPlacePrices)
        }
        val unit = Period(1, Period.Unit.MONTH, "P1M").localizedUnitPeriod(locale)
        val pricePerPeriod = localizedPricePerPeriodFull(rcPackage, locale, showZeroDecimalPlacePrices)
        val pricePerMonth = localizedPricePerMonth(rcPackage, locale, showZeroDecimalPlacePrices)
        return "$pricePerPeriod ($pricePerMonth/$unit)"
    }

    @SuppressWarnings("MagicNumber")
    fun localizedRelativeDiscount(discountRelativeToMostExpensivePerMonth: Double?): String? {
        return resourceProvider.localizedDiscount(discountRelativeToMostExpensivePerMonth)
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
    resourceProvider: ResourceProvider,
): String? {
    return resourceProvider.localizedDiscount(discountRelativeToMostExpensivePerMonth)
}

@SuppressWarnings("MagicNumber")
private fun ResourceProvider.localizedDiscount(
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
