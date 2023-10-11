package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct

@Suppress("unused")
private class TestStoreProductAPI {
    fun checkConstructor(price: Price, period: Period?) {
        TestStoreProduct(
            id = "ID",
            title = "title",
            price = price,
            description = "description",
            period = period,
        )
        TestStoreProduct(
            id = "ID",
            title = "title",
            price = price,
            description = "description",
            period = period,
            freeTrialPeriod = period,
        )
        TestStoreProduct(
            id = "ID",
            title = "title",
            price = price,
            description = "description",
            period = period,
            introPrice = null,
        )
        TestStoreProduct(
            id = "ID",
            title = "title",
            price = price,
            description = "description",
            period = period,
            introPrice = price,
        )
    }
}
