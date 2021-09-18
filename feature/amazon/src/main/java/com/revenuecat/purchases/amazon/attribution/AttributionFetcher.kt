package com.revenuecat.purchases.amazon.attribution

import android.app.Application
import android.provider.Settings
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.attribution.AttributionFetcherInterface
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.AttributionStrings

class AttributionFetcher : AttributionFetcherInterface {

    override fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (advertisingID: String?, androidID: String?) -> Unit
    ) {
        var advertisingID: String? = null
        return try {
            val contentResolver = applicationContext.contentResolver

            val limitAdTrackingSetting = Settings.Secure.getInt(contentResolver, "limit_ad_tracking")
            val isLimitAdTrackingEnabled = limitAdTrackingSetting != 0
            if (!isLimitAdTrackingEnabled) {
                advertisingID = Settings.Secure.getString(contentResolver, "advertising_id")
            }
            completion(advertisingID, null)
        } catch (e: Settings.SettingNotFoundException) {
            log(LogIntent.AMAZON_ERROR,
                AttributionStrings.AMAZON_COULD_NOT_GET_ADID.format(e.localizedMessage))
            completion(null, null)
        }
    }
}
