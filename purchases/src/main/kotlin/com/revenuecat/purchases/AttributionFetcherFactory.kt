package com.revenuecat.purchases

import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.google.attribution.GoogleDeviceIdentifiersFetcher

object AttributionFetcherFactory {

    fun createAttributionFetcher(
        store: Store,
        dispatcher: Dispatcher,
    ) = when (store) {
        Store.PLAY_STORE -> GoogleDeviceIdentifiersFetcher(dispatcher)
        Store.AMAZON -> {
            try {
                Class.forName("com.revenuecat.purchases.amazon.attribution.AmazonDeviceIdentifiersFetcher")
                    .getConstructor()
                    .newInstance() as DeviceIdentifiersFetcher
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
