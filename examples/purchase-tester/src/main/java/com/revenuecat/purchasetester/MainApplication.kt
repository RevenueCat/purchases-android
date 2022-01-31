package com.revenuecat.purchasetester

import android.app.Activity
import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.amazon.AmazonConfiguration
import java.lang.IllegalArgumentException

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

        val purchasesConfigurationBuilder =
            when {
                GOOGLE_API_KEY.isNotEmpty() -> PurchasesConfiguration.Builder(this, GOOGLE_API_KEY)
                AMAZON_API_KEY.isNotEmpty() -> AmazonConfiguration.Builder(this, AMAZON_API_KEY)
                else -> {
                    throw IllegalArgumentException("Set at least one API key in Constants.")
                }
            }
        Purchases.configure(purchasesConfigurationBuilder.build())
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

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = ContextCompat.getSystemService(context, ClipboardManager::class.java)
    val clip = ClipData.newPlainText(label, text)
    clipboard?.primaryClip = clip
}
