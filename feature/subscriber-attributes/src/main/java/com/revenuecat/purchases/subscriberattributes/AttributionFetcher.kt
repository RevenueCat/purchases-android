package com.revenuecat.purchases.subscriberattributes

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.errorLog

class AttributionFetcher {

    fun getDeviceIdentifiers(applicationContext: Application): Pair<String?, String> {
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
        @SuppressLint("HardwareIds")
        val androidID =
            Settings.Secure.getString(applicationContext.contentResolver, Settings.Secure.ANDROID_ID)
        return advertisingID to androidID
    }

}
