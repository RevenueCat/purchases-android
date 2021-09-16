package com.revenuecat.purchases

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.attribution.AttributionFetcherInterface
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.subscriberattributes.AttributionFetcher

object AttributionFetcherFactory {

    fun createAttributionFetcher(
        store: Store,
        dispatcher: Dispatcher
    ) = when (store) {
        Store.PLAY_STORE -> AttributionFetcher(dispatcher)
        Store.AMAZON -> {
            try {
                Class.forName("com.revenuecat.purchases.amazon.attribution.AttributionFetcher")
                    .getConstructor()
                    .newInstance() as AttributionFetcherInterface
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
