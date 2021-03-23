package com.revenuecat.purchases

import android.app.Application
import android.content.Context
import android.os.Handler
import androidx.annotation.VisibleForTesting
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.google.BillingWrapper

@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
object BillingFactory {

    fun createBilling(
        store: Store,
        application: Application,
        backend: Backend,
        cache: DeviceCache
    ) = when (store) {
        Store.PLAY_STORE -> BillingWrapper(
            BillingWrapper.ClientFactory(application),
            Handler(application.mainLooper),
            cache
        )
        Store.AMAZON -> {
            try {
                Class.forName("com.revenuecat.purchases.amazon.AmazonBilling")
                    .getConstructor(Context::class.java, Backend::class.java, DeviceCache::class.java)
                    .newInstance(application.applicationContext, backend, cache) as BillingAbstract
            } catch (e: ClassNotFoundException) {
                errorLog("Make sure purchases-amazon is added as dependency")
                throw e
            }
        }
        else -> {
            errorLog("Incompatible store ($store) used")
            throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
        }
    }
}
