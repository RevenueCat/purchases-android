package com.revenuecat.apitester.kotlin

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.TestStoreProduct

@Suppress("unused", "UNUSED_VARIABLE", "LongMethod", "DEPRECATION")
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
        TestStoreProduct(
            id = "ID",
            name = "name",
            title = "title",
            price = price,
            description = "description",
            period = period,
        )
        TestStoreProduct(
            id = "ID",
            name = "name",
            title = "title",
            price = price,
            description = "description",
            period = period,
            freeTrialPeriod = period,
        )
        TestStoreProduct(
            id = "ID",
            name = "name",
            title = "title",
            price = price,
            description = "description",
            period = period,
            introPrice = null,
        )
        TestStoreProduct(
            id = "ID",
            name = "name",
            title = "title",
            price = price,
            description = "description",
            period = period,
            introPrice = price,
        )
    }

    fun checkTestStoreProductIsStoreProduct(testStoreProduct: TestStoreProduct) {
        val storeProduct: StoreProduct = testStoreProduct
    }
}
