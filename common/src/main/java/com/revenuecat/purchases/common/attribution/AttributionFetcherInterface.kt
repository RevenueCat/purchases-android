package com.revenuecat.purchases.common.attribution

import android.app.Application

interface AttributionFetcherInterface {

    fun getDeviceIdentifiers(
        applicationContext: Application,
        completion: (advertisingID: String?, androidID: String) -> Unit
    )
}
