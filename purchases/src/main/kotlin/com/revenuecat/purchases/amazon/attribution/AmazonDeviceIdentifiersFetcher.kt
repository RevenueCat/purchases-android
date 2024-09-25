package com.revenuecat.purchases.amazon.attribution

import android.app.Application
import android.provider.Settings
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.utils.filterNotNullValues

internal class AmazonDeviceIdentifiersFetcher : DeviceIdentifiersFetcher {

    override fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    ) {
        var advertisingID: String? = null
        try {
            val contentResolver = applicationContext.contentResolver

            val limitAdTrackingSetting = Settings.Secure.getInt(contentResolver, "limit_ad_tracking")
            val isLimitAdTrackingDisabled = limitAdTrackingSetting == 0
            if (isLimitAdTrackingDisabled) {
                advertisingID = Settings.Secure.getString(contentResolver, "advertising_id")
            }
        } catch (e: Settings.SettingNotFoundException) {
            log(
                LogIntent.AMAZON_ERROR,
                AttributionStrings.AMAZON_COULD_NOT_GET_ADID.format(e.localizedMessage),
            )
        }
        val deviceIdentifiers = mapOf(
            SubscriberAttributeKey.DeviceIdentifiers.AmazonAdID.backendKey to advertisingID,
            SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey to "true",
            SubscriberAttributeKey.DeviceIdentifiers.DeviceVersion.backendKey to "true",
        ).filterNotNullValues()
        completion(deviceIdentifiers)
    }
}
