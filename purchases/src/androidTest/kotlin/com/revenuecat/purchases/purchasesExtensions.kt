package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.BillingAbstract

internal fun Purchases.Companion.configure(
    configuration: PurchasesConfiguration,
    overrideBillingAbstract: BillingAbstract,
    forceServerErrors: Boolean = false,
    forceSigningErrors: Boolean = false,
): Purchases {
    return PurchasesFactory(isDebugBuild = { false }).createPurchases(
        configuration,
        platformInfo,
        proxyURL,
        overrideBillingAbstract,
        forceServerErrors,
        forceSigningErrors,
        runningIntegrationTests = true,
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
    get() = purchasesOrchestrator.appConfig.forceServerErrors
    set(value) {
        purchasesOrchestrator.appConfig.forceServerErrors = value
    }

var Purchases.forceSigningErrors: Boolean
    get() = purchasesOrchestrator.appConfig.forceSigningErrors
    set(value) {
        purchasesOrchestrator.appConfig.forceSigningErrors = value
    }
