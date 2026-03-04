package com.revenuecat.purchases.common.offerings

import com.revenuecat.purchases.PackageType
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.PricingPhase
import com.revenuecat.purchases.models.RecurrenceMode
import com.revenuecat.purchases.models.TestStoreProduct
import java.util.Locale

internal enum class PreviewProductSpec(
    val displayName: String,
    val price: Double,
    val period: Period?,
    val freeTrialPeriod: Period? = null,
) {
    LIFETIME("Lifetime", PRICE_LIFETIME, null),
    ANNUAL(
        "Annual",
        PRICE_ANNUAL,
        period = Period(value = 1, unit = Period.Unit.YEAR, iso8601 = "P1Y"),
        freeTrialPeriod = Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W"),
    ),
    SIX_MONTH("6 Month", PRICE_SIX_MONTH, Period(value = 6, unit = Period.Unit.MONTH, iso8601 = "P6M")),
    THREE_MONTH("3 Month", PRICE_THREE_MONTH, Period(value = 3, unit = Period.Unit.MONTH, iso8601 = "P3M")),
    TWO_MONTH("2 Month", PRICE_TWO_MONTH, Period(value = 2, unit = Period.Unit.MONTH, iso8601 = "P2M")),
    MONTHLY("Monthly", PRICE_MONTHLY, Period(value = 1, unit = Period.Unit.MONTH, iso8601 = "P1M")),
    WEEKLY("Weekly", PRICE_WEEKLY, Period(value = 1, unit = Period.Unit.WEEK, iso8601 = "P1W")),
    DEFAULT("Product", PRICE_DEFAULT, null),
    ;

    fun toTestStoreProduct(productId: String): TestStoreProduct {
        val priceMicros = Math.round(price * MICROS_PER_DOLLAR)
        val priceFormatted = "$${String.format(Locale.US, "%.2f", price)}"
        val freeTrialPricingPhase = freeTrialPeriod?.let {
            PricingPhase(
                billingPeriod = it,
                recurrenceMode = RecurrenceMode.FINITE_RECURRING,
                billingCycleCount = 1,
                price = Price(
                    amountMicros = 0L,
                    currencyCode = CURRENCY_CODE,
                    formatted = FREE_TRIAL_PRICE_FORMATTED,
                ),
            )
        }
        return TestStoreProduct(
            id = productId,
            name = displayName,
            title = displayName,
            description = displayName,
            price = Price(amountMicros = priceMicros, currencyCode = CURRENCY_CODE, formatted = priceFormatted),
            period = period,
            freeTrialPricingPhase = freeTrialPricingPhase,
        )
    }

    companion object {
        private const val CURRENCY_CODE = "USD"
        private const val MICROS_PER_DOLLAR = 1_000_000L
        private const val FREE_TRIAL_PRICE_FORMATTED = "Free"

        private const val PRICE_LIFETIME = 199.99
        private const val PRICE_ANNUAL = 59.99
        private const val PRICE_SIX_MONTH = 30.99
        private const val PRICE_THREE_MONTH = 15.99
        private const val PRICE_TWO_MONTH = 11.49
        private const val PRICE_MONTHLY = 5.99
        private const val PRICE_WEEKLY = 1.99
        private const val PRICE_DEFAULT = 249.99

        fun fromPackageType(packageType: PackageType): PreviewProductSpec = when (packageType) {
            PackageType.LIFETIME -> LIFETIME
            PackageType.ANNUAL -> ANNUAL
            PackageType.SIX_MONTH -> SIX_MONTH
            PackageType.THREE_MONTH -> THREE_MONTH
            PackageType.TWO_MONTH -> TWO_MONTH
            PackageType.MONTHLY -> MONTHLY
            PackageType.WEEKLY -> WEEKLY
            else -> DEFAULT
        }

        fun inferFromProductId(productId: String): PackageType {
            val id = productId.lowercase()
            return when {
                id.containsAny("lifetime", "forever", "permanent") -> PackageType.LIFETIME
                id.containsAny("annual", "year") -> PackageType.ANNUAL
                id.containsAny("six_month", "sixmonth", "6month", "semester") -> PackageType.SIX_MONTH
                id.containsAny("three_month", "threemonth", "3month", "quarter") -> PackageType.THREE_MONTH
                id.containsAny("two_month", "twomonth", "2month", "bimonth") -> PackageType.TWO_MONTH
                id.contains("month") -> PackageType.MONTHLY
                id.contains("week") -> PackageType.WEEKLY
                else -> PackageType.CUSTOM
            }
        }

        private fun String.containsAny(vararg keywords: String): Boolean =
            keywords.any { this.contains(it) }
    }
}
