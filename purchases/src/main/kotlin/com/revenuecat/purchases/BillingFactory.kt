package com.revenuecat.purchases

import android.app.Application
import android.os.Handler
import com.revenuecat.purchases.amazon.AmazonBilling
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.google.BillingWrapper
import com.revenuecat.purchases.teststore.TestStoreBillingWrapper

internal object BillingFactory {

    @Suppress("LongParameterList")
    fun createBilling(
        store: Store,
        application: Application,
        backendHelper: BackendHelper,
        cache: DeviceCache,
        finishTransactions: Boolean,
        diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
        stateProvider: PurchasesStateProvider,
        pendingTransactionsForPrepaidPlansEnabled: Boolean,
        backend: Backend,
    ): BillingAbstract {
        return when (store) {
            Store.PLAY_STORE -> BillingWrapper(
                BillingWrapper.ClientFactory(application, pendingTransactionsForPrepaidPlansEnabled),
                Handler(application.mainLooper),
                cache,
                diagnosticsTrackerIfEnabled,
                stateProvider,
            )
            Store.AMAZON -> {
                try {
                    AmazonBilling(
                        application.applicationContext,
                        cache,
                        finishTransactions,
                        Handler(application.mainLooper),
                        backendHelper,
                        stateProvider,
                        diagnosticsTrackerIfEnabled,
                    )
                } catch (e: NoClassDefFoundError) {
                    errorLog(e) { "Make sure purchases-amazon is added as dependency" }
                    throw e
                }
            }
            Store.TEST_STORE -> TestStoreBillingWrapper(
                deviceCache = cache,
                mainHandler = Handler(application.mainLooper),
                purchasesStateProvider = stateProvider,
                backend = backend,
            )
            else -> {
                errorLog { "Incompatible store ($store) used" }
                throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
            }
        }
    }
}
