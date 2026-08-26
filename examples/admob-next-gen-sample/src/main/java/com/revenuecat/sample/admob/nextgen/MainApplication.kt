package com.revenuecat.sample.admob.nextgen

import android.app.Application
import android.util.Log
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableAdsStatus = MutableStateFlow("Google Mobile Ads: initializing")

    val adsStatus: StateFlow<String> = mutableAdsStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initializeRevenueCat()
        initializeGoogleMobileAds()
    }

    private fun initializeRevenueCat() {
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
            Log.w(TAG, "Set REVENUECAT_API_KEY in local.properties to enable RevenueCat tracking")
            return
        }

        Purchases.logLevel = LogLevel.DEBUG
        Purchases.configure(
            PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build(),
        )
    }

    private fun initializeGoogleMobileAds() {
        applicationScope.launch {
            runCatching {
                MobileAds.initialize(
                    this@MainApplication,
                    InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
                ) {
                    mutableAdsStatus.value = "Google Mobile Ads: ready"
                }
            }.onFailure { error ->
                Log.e(TAG, "Google Mobile Ads initialization failed", error)
                mutableAdsStatus.value = "Google Mobile Ads: ${error.message ?: "initialization failed"}"
            }
        }
    }

    private companion object {
        const val TAG = "AdMobNextGenSample"
    }
}
