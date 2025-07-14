package com.revenuecat.purchases.ui.revenuecatui.extensions

import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct

@Suppress("LongParameterList")
internal fun TestStoreProduct.copy(
    id: String = this.id,
    name: String = this.name,
    title: String = this.title,
    description: String = this.description,
    price: Price = this.price,
    period: Period? = this.period,
): TestStoreProduct = TestStoreProduct(
    id = id,
    name = name,
    title = title,
    description = description,
    price = price,
    period = period,
)
