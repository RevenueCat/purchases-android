package com.revenuecat.purchases

import android.app.Application
import android.content.Context
import android.os.Handler
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.diagnostics.DiagnosticsTracker
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.google.BillingWrapper

@VisibleForTesting(otherwise = VisibleForTesting.PACKAGE_PRIVATE)
object BillingFactory {

    @Suppress("LongParameterList")
    fun createBilling(
        store: Store,
        application: Application,
        backend: Backend,
        cache: DeviceCache,
        observerMode: Boolean,
        diagnosticsTracker: DiagnosticsTracker?
    ) = when (store) {
        Store.PLAY_STORE -> BillingWrapper(
            BillingWrapper.ClientFactory(application),
            Handler(application.mainLooper),
            cache,
            diagnosticsTracker
        )
        Store.AMAZON -> {
            try {
                Class.forName("com.revenuecat.purchases.amazon.AmazonBilling")
                    .getConstructor(
                        Context::class.java,
                        Backend::class.java,
                        DeviceCache::class.java,
                        Boolean::class.java,
                        Handler::class.java
                    ).newInstance(
                        application.applicationContext,
                        backend,
                        cache,
                        observerMode,
                        Handler(application.mainLooper)
                    ) as BillingAbstract
            } catch (e: ClassNotFoundException) {
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
