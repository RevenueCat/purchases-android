package com.revenuecat.purchases.common.subscriberattributes

import android.app.Application

internal interface DeviceIdentifiersFetcher {

    public fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    )
}
