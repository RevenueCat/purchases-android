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
class PriceExtensionsPricePerYearTest : PriceExtensionsPricePerPeriodTest() {
    @Test
    @Parameters(
        value = [
            "$1, 1D, $365.00",
            "$2, 1D, $730.00",
            "$5, 15D, $121.67",
            "$10, 1W, $521.40",
            "$10, 2W, $260.70",
            "$14.99, 1M, $179.88",
            "$5, 1M, $60.00",
            "$30, 2M, $180.00",
            "$40, 3M, $160.00",
            "$120, 1Y, $120.00",
            "$29.99, 1Y, $29.99",
            "$50, 2Y, $25.00",
            "$720, 3Y, $240.00",
        ]
    )
    fun `pricePerYear`(
        priceString: String,
        periodString: String,
        expectedString: String,
    ) {
        test(priceString, periodString, expectedString)
    }

    override fun compute(price: Price, period: Period): Price {
        return price.pricePerYear(period, locale)
    }
}
