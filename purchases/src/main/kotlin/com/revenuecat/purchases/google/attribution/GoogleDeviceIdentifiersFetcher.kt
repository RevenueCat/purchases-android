package com.revenuecat.purchases.google.attribution

import android.app.Application
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.utils.filterNotNullValues

internal class GoogleDeviceIdentifiersFetcher(
    private val dispatcher: Dispatcher,
) : DeviceIdentifiersFetcher {
    override fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    ) {
        dispatcher.enqueue({
            val deviceIdentifiers = mapOf(
                SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey to "true",
            ).filterNotNullValues()
            completion(deviceIdentifiers)
        })
    }
}
