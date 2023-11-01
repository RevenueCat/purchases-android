package com.revenuecat.purchases.utils

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import junitparams.JUnitParamsRunner
import junitparams.Parameters
import org.junit.Test
import org.junit.experimental.categories.Category
import org.junit.runner.RunWith

@RunWith(JUnitParamsRunner::class)
@Category(AndroidJUnit4::class)
class PriceExtensionsPricePerWeekTest : PriceExtensionsPricePerPeriodTest() {
    @Test
    @Parameters(
        value = [
            "$1, 1D, $7.00",
            "$2, 14D, $1.00",
            "$10, 1W, $10.00",
            "$10, 2W, $5.00",
            "$14.99, 1M, $3.75",
            "$30, 2M, $3.75",
            "$40, 3M, $3.33",
            "$120, 1Y, $2.30",
            "$50, 1Y, $0.96",
            "$29.99, 1Y, $0.58",
            "$720, 3Y, $4.60",
        ]
    )
    fun `pricePerWeek`(
        priceString: String,
        periodString: String,
        expectedString: String,
    ) {
        test(priceString, periodString, expectedString)
    }

    override fun compute(price: Price, period: Period): Price {
        return price.pricePerWeek(period, locale)
    }
}
