package com.revenuecat.purchasetester

import android.app.Activity
import android.app.Application
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError

class MainApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        StrictMode.setVmPolicy(
            VmPolicy.Builder()
                .detectLeakedClosableObjects()
                .penaltyLog()
                .penaltyDeath()
                .build()
        )
        Purchases.debugLogsEnabled = true
        Purchases.configure(PurchasesConfiguration.Builder(this, "api_key").build())
        // set attributes to store additional, structured information for a user in RevenueCat.
        // More info: https://docs.revenuecat.com/docs/user-attributes
        Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))
    }
}

fun showError(message: String) {
    Log.e("Purchase Tester", message)
}

fun showError(error: PurchasesError) {
    showError(error.message)
}

fun showUserError(activity: Activity, error: PurchasesError) {
    MaterialAlertDialogBuilder(activity)
        .setMessage(error.message)
        .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
        .show()
}
