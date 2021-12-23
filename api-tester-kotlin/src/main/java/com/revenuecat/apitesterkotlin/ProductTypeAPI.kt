package com.revenuecat.apitesterkotlin

import com.revenuecat.purchases.ProductType

@Suppress("unused")
private class ProductTypeAPI {
    fun check(type: ProductType) {
        when (type) {
            ProductType.SUBS,
            ProductType.INAPP,
            ProductType.UNKNOWN
            -> {}
        }
    }
}
