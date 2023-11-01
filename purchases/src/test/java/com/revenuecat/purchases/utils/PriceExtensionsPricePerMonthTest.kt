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
class PriceExtensionsPricePerMonthTest: PriceExtensionsPricePerPeriodTest() {
    @Test
    @Parameters(
        value = [
            "$2, 1D, $60.00",
            "$5, 15D, $10.00",
            "$10, 1W, $40.00",
            "$10, 2W, $20.00",
            "$14.99, 1M, $14.99",
            "$30, 2M, $15.00",
            "$40, 3M, $13.33",
            "$120, 1Y, $10.00",
            "$50, 1Y, $4.17",
            "$29.99, 1Y, $2.50",
            "$720, 3Y, $20.00",
        ]
    )
    fun `pricePerMonth`(
        priceString: String,
        periodString: String,
        expectedString: String,
    ) {
        test(priceString, periodString, expectedString)
    }

    override fun compute(price: Price, period: Period): Price {
        return price.pricePerMonth(period, locale)
    }
}
