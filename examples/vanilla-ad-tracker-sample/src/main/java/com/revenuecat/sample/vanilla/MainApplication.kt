package com.revenuecat.sample.vanilla

import android.app.Application
import android.util.Log
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.sample.vanilla.data.Constants
import com.unity3d.mediation.LevelPlay
import com.unity3d.mediation.LevelPlayConfiguration
import com.unity3d.mediation.LevelPlayInitError
import com.unity3d.mediation.LevelPlayInitListener
import com.unity3d.mediation.LevelPlayInitRequest

internal sealed interface LevelPlayInitializationState {
    data object Initializing : LevelPlayInitializationState
    data object Initialized : LevelPlayInitializationState
    data class Failed(val message: String) : LevelPlayInitializationState
}

class MainApplication : Application() {

    internal val levelPlayInitializationState: MutableState<LevelPlayInitializationState> =
        mutableStateOf(LevelPlayInitializationState.Initializing)

    override fun onCreate() {
        super.onCreate()

        initializeRevenueCat()
        initializeLevelPlay()
    }

    private fun initializeRevenueCat() {
        Purchases.logLevel = LogLevel.DEBUG

        val configuration = PurchasesConfiguration.Builder(
            context = this,
            apiKey = Constants.REVENUECAT_API_KEY,
        ).build()

        Purchases.configure(configuration)

        Log.d(TAG, "RevenueCat SDK initialized. App user ID: ${Purchases.sharedInstance.appUserID}")
    }

    private fun initializeLevelPlay() {
        val request = LevelPlayInitRequest.Builder(Constants.LevelPlay.APP_KEY).build()
        LevelPlay.init(
            this,
            request,
            object : LevelPlayInitListener {
                override fun onInitSuccess(configuration: LevelPlayConfiguration) {
                    levelPlayInitializationState.value = LevelPlayInitializationState.Initialized
                    Log.d(TAG, "LevelPlay SDK initialized successfully")
                }

                override fun onInitFailed(error: LevelPlayInitError) {
                    levelPlayInitializationState.value = LevelPlayInitializationState.Failed(error.toString())
                    Log.e(TAG, "LevelPlay SDK initialization failed: $error")
                }
            },
        )
    }

    companion object {
        private const val TAG = "MainApplication"
    }
}
