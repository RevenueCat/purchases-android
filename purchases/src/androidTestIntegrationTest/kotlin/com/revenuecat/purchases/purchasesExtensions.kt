package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.BillingAbstract

fun Purchases.Companion.configure(
    configuration: PurchasesConfiguration,
    overrideBillingAbstract: BillingAbstract
): Purchases {
    return PurchasesFactory().createPurchases(
        configuration,
        platformInfo,
        proxyURL,
        overrideBillingAbstract
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
    }
}
