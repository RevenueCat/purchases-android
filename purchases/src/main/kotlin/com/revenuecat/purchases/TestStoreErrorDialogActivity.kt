package com.revenuecat.purchases

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle

internal class TestStoreErrorDialogActivity : Activity() {

    companion object {
        fun show(context: Context) {
            val intent = Intent(context, TestStoreErrorDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)

        AlertDialog.Builder(this)
            .setTitle("Wrong API Key")
            .setMessage(
                "This app is using a test API key. To prepare for release, update your RevenueCat settings to use a " +
                    "production key.\n\nFor more info, visit the RevenueCat dashboard.\n\nThe app will close now to " +
                    "protect the security of test purchases.",
            )
            .setCancelable(false)
            .setPositiveButton("OK") { _, _ -> crashApp() }
            .show()
    }

    override fun onBackPressed() {
        crashApp()
    }

    override fun onPause() {
        super.onPause()
        crashApp()
    }

    private fun crashApp() {
        throw PurchasesException(
            error = PurchasesError(code = PurchasesErrorCode.ConfigurationError),
            overridenMessage = "Test Store API key used in release build. Please configure the " +
                "Play Store app on the RevenueCat dashboard and use its corresponding Google API key " +
                "before releasing. Visit https://rev.cat/sdk-test-store to learn more.",
        )
    }
}
