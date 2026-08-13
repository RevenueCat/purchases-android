package com.revenuecat.purchases.admob.nextgen.tracking

import com.revenuecat.purchases.Purchases

internal object PurchasesTestHelper {
    private val backingField = Purchases::class.java.getDeclaredField("backingFieldSharedInstance").apply {
        isAccessible = true
    }

    fun setSharedInstance(instance: Purchases?) {
        backingField.set(null, instance)
    }
}
