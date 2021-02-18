package com.revenuecat.sample

import android.app.Application
import android.util.Log
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.amazon.AmazonConfiguration

const val PREMIUM_ENTITLEMENT_ID = "pro_cat"

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Doesn't work with amazon becuase they have issues
//        StrictMode.setVmPolicy(
//            VmPolicy.Builder()
//                .detectLeakedClosableObjects()
//                .penaltyLog()
//                .penaltyDeath()
//                .build()
//        )
        Purchases.debugLogsEnabled = true
        val configuration =
            AmazonConfiguration.Builder(context = this, apiKey = "api_key")
                .build()

        Purchases.configure(configuration)

        // set attributes to store additional, structured information for a user in RevenueCat.
        // More info: https://docs.revenuecat.com/docs/user-attributes
        Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))
    }
}

fun showError(message: String) {
    Log.e("Purchases Sample", message)
}

fun showError(error: PurchasesError) {
    showError(error.message)
}
