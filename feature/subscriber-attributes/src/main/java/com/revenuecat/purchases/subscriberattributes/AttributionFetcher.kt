package com.revenuecat.purchases.subscriberattributes

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.strings.AttributionStrings
import java.io.IOException
import java.util.concurrent.TimeoutException

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
            log(LogIntent.GOOGLE_ERROR,
                    AttributionStrings.GOOGLE_PLAY_SERVICES_NOT_INSTALLED_FETCHING_ADVERTISING_IDENTIFIER
                            .format(e.localizedMessage))
        } catch (e: GooglePlayServicesRepairableException) {
            log(LogIntent.GOOGLE_ERROR,
                    AttributionStrings.GOOGLE_PLAY_SERVICES_REPAIRABLE_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                            .format(e.localizedMessage))
        } catch (e: TimeoutException) {
            log(LogIntent.GOOGLE_ERROR,
                    AttributionStrings.TIMEOUT_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                            .format(e.localizedMessage))
        } catch (e: IOException) {
            log(LogIntent.GOOGLE_ERROR,
                    AttributionStrings.IO_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                            .format(e.localizedMessage))
        }
        return advertisingID
    }
}
