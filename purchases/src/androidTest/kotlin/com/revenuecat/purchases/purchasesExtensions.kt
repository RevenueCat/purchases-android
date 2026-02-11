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
        baseUrlString = when (Constants.backendEnvironment) {
            Constants.BackendEnvironment.LOAD_SHEDDER_US_EAST_2 -> "https://fortress-us-east-2.revenuecat.com/"
            Constants.BackendEnvironment.LOAD_SHEDDER_US_EAST_1 -> "https://fortress-us-east-1.revenuecat.com/"
            else -> AppConfig.baseUrlString
        },
    ).also {
        @SuppressLint("RestrictedApi")
        sharedInstance = it
    }
}

public fun Purchases.Companion.resetSingleton() {
    backingFieldSharedInstance?.close()
    backingFieldSharedInstance = null
}

var Purchases.forceSigningErrors: Boolean
    get() = purchasesOrchestrator.appConfig.forceSigningErrors
    set(value) {
        purchasesOrchestrator.appConfig.forceSigningErrors = value
    }
