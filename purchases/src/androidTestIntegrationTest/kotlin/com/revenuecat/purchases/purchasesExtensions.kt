package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.BillingAbstract
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

fun Purchases.Companion.configure(
    configuration: PurchasesConfiguration,
    overrideBillingAbstract: BillingAbstract,
    forceServerErrors: Boolean = false
): Purchases {
    return PurchasesFactory().createPurchases(
        configuration,
        platformInfo,
        proxyURL,
        overrideBillingAbstract,
        forceServerErrors
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
    }
}

fun Purchases.waitForProductEntitlementMapping() {
    val latch = CountDownLatch(1)
    offlineEntitlementsManager.updateProductEntitlementMappingCacheIfStale {
        latch.countDown()
    }
    latch.await(2, TimeUnit.SECONDS)
}

fun Purchases.waitForCustomerInfoSync() {
    val latch = CountDownLatch(1)
    getCustomerInfoWith(
        onError = { latch.countDown() },
        onSuccess = { latch.countDown() }
    )
    latch.await(2, TimeUnit.SECONDS)
}

var Purchases.forceServerErrors: Boolean
    get() = appConfig.forceServerErrors
    set(value) {
        appConfig.forceServerErrors = value
    }
