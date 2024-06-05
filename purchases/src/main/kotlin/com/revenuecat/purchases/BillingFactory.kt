package com.revenuecat.purchases

import android.app.Application
import android.os.Handler
import com.revenuecat.purchases.amazon.AmazonBilling
import com.revenuecat.purchases.common.BackendHelper
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.google.BillingWrapper

internal object BillingFactory {

    @Suppress("LongParameterList")
    fun createBilling(
        store: Store,
        application: Application,
        backendHelper: BackendHelper,
        cache: DeviceCache,
        observerMode: Boolean,
        diagnosticsTrackerIfEnabled: DiagnosticsTracker?,
        stateProvider: PurchasesStateProvider,
        pendingPrepaidSubscriptionsEnabled: Boolean,
    ) = when (store) {
        Store.PLAY_STORE -> BillingWrapper(
            BillingWrapper.ClientFactory(application, pendingPrepaidSubscriptionsEnabled),
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
                    observerMode,
                    Handler(application.mainLooper),
                    backendHelper,
                    stateProvider,
                    diagnosticsTrackerIfEnabled,
                )
            } catch (e: NoClassDefFoundError) {
                errorLog("Make sure purchases-amazon is added as dependency", e)
                throw e
            }
        }
        else -> {
            errorLog("Incompatible store ($store) used")
            throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
        }
    }
}
