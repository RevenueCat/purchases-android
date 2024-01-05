package com.revenuecat.purchases.google.attribution

import android.annotation.SuppressLint
import android.app.Application
import android.provider.Settings
import com.google.android.gms.ads.identifier.AdvertisingIdClient
import com.google.android.gms.common.GooglePlayServicesNotAvailableException
import com.google.android.gms.common.GooglePlayServicesRepairableException
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.LogIntent
import com.revenuecat.purchases.common.log
import com.revenuecat.purchases.common.subscriberattributes.DeviceIdentifiersFetcher
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.strings.AttributionStrings
import com.revenuecat.purchases.utils.filterNotNullValues
import java.io.IOException
import java.util.concurrent.TimeoutException

internal class GoogleDeviceIdentifiersFetcher(
    private val dispatcher: Dispatcher,
) : DeviceIdentifiersFetcher {

    private val noPermissionAdvertisingIdValue = "00000000-0000-0000-0000-000000000000"

    override fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (deviceIdentifiers: Map<String, String>) -> Unit,
    ) {
        dispatcher.enqueue({
            val advertisingID: String? = getAdvertisingID(applicationContext)
            val androidID = getAndroidID(applicationContext)
            val deviceIdentifiers = mapOf(
                SubscriberAttributeKey.DeviceIdentifiers.GPSAdID.backendKey to advertisingID,
                SubscriberAttributeKey.DeviceIdentifiers.AndroidID.backendKey to androidID,
                SubscriberAttributeKey.DeviceIdentifiers.IP.backendKey to "true",
            ).filterNotNullValues()
            completion(deviceIdentifiers)
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
                if (adInfo.id == noPermissionAdvertisingIdValue) {
                    log(
                        LogIntent.WARNING,
                        AttributionStrings.GOOGLE_PLAY_ADVERTISING_ID_NOT_AVAILABLE,
                    )
                } else {
                    advertisingID = adInfo.id
                }
            }
        } catch (e: GooglePlayServicesNotAvailableException) {
            log(
                LogIntent.GOOGLE_ERROR,
                AttributionStrings.GOOGLE_PLAY_SERVICES_NOT_INSTALLED_FETCHING_ADVERTISING_IDENTIFIER
                    .format(e.localizedMessage),
            )
        } catch (e: GooglePlayServicesRepairableException) {
            log(
                LogIntent.GOOGLE_ERROR,
                AttributionStrings.GOOGLE_PLAY_SERVICES_REPAIRABLE_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                    .format(e.localizedMessage),
            )
        } catch (e: TimeoutException) {
            log(
                LogIntent.GOOGLE_ERROR,
                AttributionStrings.TIMEOUT_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                    .format(e.localizedMessage),
            )
        } catch (e: IOException) {
            log(
                LogIntent.GOOGLE_ERROR,
                AttributionStrings.IO_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                    .format(e.localizedMessage),
            )
        } catch (@Suppress("TooGenericExceptionCaught") e: NullPointerException) {
            log(
                LogIntent.GOOGLE_ERROR,
                AttributionStrings.NULL_EXCEPTION_WHEN_FETCHING_ADVERTISING_IDENTIFIER
                    .format(e.localizedMessage),
            )
        }
        return advertisingID
    }
}
