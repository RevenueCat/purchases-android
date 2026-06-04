package com.revenuecat.purchases

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.revenuecat.purchases.common.errorLog

internal class SimulatedStoreErrorDialogActivity : Activity() {

    companion object {
        private const val redactedApiKeyExtra = "redactedApiKey"

        fun show(context: Context, redactedApiKey: String) {
            val intent = Intent(context, SimulatedStoreErrorDialogActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                putExtra(redactedApiKeyExtra, redactedApiKey)
            }
            context.startActivity(intent)
        }
    }

    val redactedApiKey: String
        get() = intent.getStringExtra(redactedApiKeyExtra) ?: ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setFinishOnTouchOutside(false)

        AlertDialog.Builder(this)
            .setTitle("Wrong API Key")
            .setMessage(
                "This app is using a test API key: $redactedApiKey.\n\nTo prepare for release, update your RevenueCat" +
                    " settings to use a production key.\n\nFor more info, visit the RevenueCat dashboard.\n\nThe" +
                    " app will close now to protect the security of test purchases.",
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
        if (wasLaunchedThroughSDK()) {
            throw PurchasesException(
                error = PurchasesError(code = PurchasesErrorCode.ConfigurationError),
                overridenMessage = "Test Store API key used in release build: $redactedApiKey. Please configure the " +
                    "Play Store/Amazon app on the RevenueCat dashboard and use its corresponding API key " +
                    "before releasing. Visit https://rev.cat/sdk-test-store to learn more.",
            )
        } else {
            errorLog {
                "SimulatedStoreErrorDialogActivity was launched incorrectly. " +
                    "This activity is only meant to be launched internally by the SDK."
            }
            finish()
        }
    }

    private fun wasLaunchedThroughSDK(): Boolean {
        return intent.hasExtra(redactedApiKeyExtra)
    }
}
