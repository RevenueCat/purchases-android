package com.revenuecat.purchases.galaxy.attribution

import android.app.Application
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher

internal class GalaxyDeviceIdentifiersFetcher : DeviceIdentifiersFetcher {

    override fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    ) {
        // Samsung IAP library doesn't support fetching device identifiers, so this is a no-op
        completion(emptyMap())
    }
}
