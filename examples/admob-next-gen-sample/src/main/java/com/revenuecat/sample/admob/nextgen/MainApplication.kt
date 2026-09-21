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

internal data class InitializationStatus(
    val message: String,
    val ready: Boolean = false,
)

class MainApplication : Application() {
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val mutableRevenueCatStatus = MutableStateFlow(InitializationStatus("RevenueCat: initializing"))
    private val mutableAdsStatus = MutableStateFlow(InitializationStatus("Google Mobile Ads: initializing"))

    internal val revenueCatStatus: StateFlow<InitializationStatus> = mutableRevenueCatStatus.asStateFlow()
    internal val adsStatus: StateFlow<InitializationStatus> = mutableAdsStatus.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        initializeRevenueCat()
        initializeGoogleMobileAds()
    }

    private fun initializeRevenueCat() {
        if (BuildConfig.REVENUECAT_API_KEY.isBlank()) {
            Log.w(TAG, "Set REVENUECAT_API_KEY in local.properties to enable RevenueCat tracking")
            mutableRevenueCatStatus.value = InitializationStatus("RevenueCat: missing REVENUECAT_API_KEY")
            return
        }

        runCatching {
            Purchases.logLevel = LogLevel.DEBUG
            Purchases.configure(
                PurchasesConfiguration.Builder(this, BuildConfig.REVENUECAT_API_KEY).build(),
            )
        }.onSuccess {
            mutableRevenueCatStatus.value = InitializationStatus("RevenueCat: ready", ready = true)
        }.onFailure { error ->
            Log.e(TAG, "RevenueCat initialization failed", error)
            mutableRevenueCatStatus.value = InitializationStatus(
                "RevenueCat: ${error.message ?: "initialization failed"}",
            )
        }
    }

    private fun initializeGoogleMobileAds() {
        applicationScope.launch {
            runCatching {
                MobileAds.initialize(
                    this@MainApplication,
                    InitializationConfig.Builder(BuildConfig.ADMOB_APP_ID).build(),
                ) {
                    mutableAdsStatus.value = InitializationStatus("Google Mobile Ads: ready", ready = true)
                }
            }.onFailure { error ->
                Log.e(TAG, "Google Mobile Ads initialization failed", error)
                mutableAdsStatus.value = InitializationStatus(
                    "Google Mobile Ads: ${error.message ?: "initialization failed"}",
                )
            }
        }
    }

    private companion object {
        const val TAG = "AdMobNextGenSample"
    }
}
