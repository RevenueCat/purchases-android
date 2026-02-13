package com.revenuecat.purchases.common.subscriberattributes

import android.app.Application

internal interface DeviceIdentifiersFetcher {

    fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    )
}
