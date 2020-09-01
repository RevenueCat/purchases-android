package com.revenuecat.purchases.subscriberattributes

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.errorLog

class AttributionFetcher(
    private val dispatcher: Dispatcher
) {

    fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (advertisingID: String?, androidID: String) -> Unit
    ) {
        dispatcher.enqueue(Runnable {
            val advertisingID: String? = getAdvertisingID(applicationContext)
            val androidID = getAndroidID(applicationContext)
            completion(advertisingID, androidID)
        })
    }

    @SuppressLint("HardwareIds")
    private fun getAndroidID(applicationContext: Application) =
        Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)

    private fun getAdvertisingID(applicationContext: Application): String? {
        var advertisingID: String? = null
        try {
            val adInfo = AdvertisingIdClient.getAdvertisingIdInfo(applicationContext)
            if (!adInfo.isLimitAdTrackingEnabled) {
                advertisingID = adInfo.id
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            errorLog(
                "GooglePlayServices is not installed. Couldn't get and advertising identifier. " +
                    "Message: ${e.localizedMessage}"
            )
        } catch (e: GooglePlayServicesRepairableException) {
            errorLog(
                "GooglePlayServicesRepairableException when getting advertising identifier. " +
                    "Message: ${e.localizedMessage}"
            )
        }
        return advertisingID
    }
}
