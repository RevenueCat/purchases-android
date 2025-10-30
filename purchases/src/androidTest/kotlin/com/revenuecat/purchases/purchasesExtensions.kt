package com.revenuecat.purchases

import android.annotation.SuppressLint
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.BillingAbstract

internal fun Purchases.Companion.configure(
    configuration: PurchasesConfiguration,
    overrideBillingAbstract: BillingAbstract,
    forceServerErrorStrategy: ForceServerErrorStrategy? = null,
    forceSigningErrors: Boolean = false,
): Purchases {
    return PurchasesFactory(isDebugBuild = { true }).createPurchases(
        configuration,
        platformInfo,
        proxyURL,
        overrideBillingAbstract,
        forceServerErrorStrategy,
        forceSigningErrors,
        runningIntegrationTests = true,
        baseUrlString = if (Constants.testSuite != Constants.TestSuite.LOAD_SHEDDER_US_EAST_2) {
            AppConfig.baseUrlString
        } else {
            "https://fortress2.revenuecat.com/"
        },
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
    }
}

fun Purchases.Companion.resetSingleton() {
    backingFieldSharedInstance?.close()
    backingFieldSharedInstance = null
}

var Purchases.forceSigningErrors: Boolean
    get() = purchasesOrchestrator.appConfig.forceSigningErrors
    set(value) {
        purchasesOrchestrator.appConfig.forceSigningErrors = value
    }
