package com.revenuecat.purchases

import com.revenuecat.purchases.amazon.attribution.AmazonDeviceIdentifiersFetcher
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.errorLog
import com.revenuecat.purchases.galaxy.attribution.GalaxyDeviceIdentifiersFetcher
import com.revenuecat.purchases.google.attribution.GoogleDeviceIdentifiersFetcher

internal object AttributionFetcherFactory {

    fun createAttributionFetcher(
        store: Store,
        dispatcher: Dispatcher,
    ) = when (store) {
        Store.PLAY_STORE -> GoogleDeviceIdentifiersFetcher(dispatcher)
        Store.AMAZON -> AmazonDeviceIdentifiersFetcher()
        Store.GALAXY -> GalaxyDeviceIdentifiersFetcher()
        else -> {
            errorLog { "Incompatible store ($store) used" }
            throw IllegalArgumentException("Couldn't configure SDK. Incompatible store ($store) used")
        }
    }
}
