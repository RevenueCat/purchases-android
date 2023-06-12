package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.BillingAbstract

fun Purchases.Companion.configure(
    configuration: PurchasesConfiguration,
    overrideBillingAbstract: BillingAbstract,
    forceServerErrors: Boolean = false,
): Purchases {
    return PurchasesFactory().createPurchases(
        configuration,
        platformInfo,
        proxyURL,
        overrideBillingAbstract,
        forceServerErrors,
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
    }
}

fun Purchases.Companion.resetSingleton() {
    backingFieldSharedInstance?.close()
    backingFieldSharedInstance = null
}

var Purchases.forceServerErrors: Boolean
    get() = appConfig.forceServerErrors
    set(value) {
        appConfig.forceServerErrors = value
    }
