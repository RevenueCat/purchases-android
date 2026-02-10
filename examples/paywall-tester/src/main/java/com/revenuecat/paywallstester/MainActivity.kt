@file:OptIn(ExperimentalComposeUiApi::class)

package com.revenuecat.paywallstester

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.revenuecat.paywallstester.ui.theme.PaywallTesterAndroidTheme
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.customercenter.ShowCustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

class MainActivity : ComponentActivity(), PaywallResultHandler {
    companion object {
        private const val TAG = "PaywallsTester"
    }

    private lateinit var paywallActivityLauncher: PaywallActivityLauncher
    private val customerCenter = registerForActivityResult(ShowCustomerCenter()) {}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paywallActivityLauncher = PaywallActivityLauncher(this, this)
        setContent {
            PaywallTesterAndroidTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    PaywallTesterApp()
                }
            }
        }
    }

    override fun onActivityResult(result: PaywallResult) {
        // TODO-PAYWALLS: Handle result
        Log.e(TAG, "LAUNCH PAYWALL RESULT: $result")
    }

    fun launchPaywall(offering: Offering? = null, edgeToEdge: Boolean = false) {
        val options = PaywallActivityLaunchOptions.Builder()
            .setOffering(offering)
            .setEdgeToEdge(edgeToEdge)
            .setListener(paywallListener)
            .build()
        paywallActivityLauncher.launchWithOptions(options)
    }

    private val paywallListener = object : PaywallListener {
        override fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
            Log.d(TAG, "onPurchasePackageInitiated: ${rcPackage.identifier}")
            resume()
        }

        override fun onPurchaseStarted(rcPackage: Package) {
            Log.d(TAG, "onPurchaseStarted: ${rcPackage.identifier}")
        }

        override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
            Log.d(TAG, "onPurchaseCompleted: ${storeTransaction.orderId}")
        }

        override fun onPurchaseError(error: PurchasesError) {
            Log.d(TAG, "onPurchaseError: ${error.message}")
        }

        override fun onPurchaseCancelled() {
            Log.d(TAG, "onPurchaseCancelled")
        }

        override fun onRestoreStarted() {
            Log.d(TAG, "onRestoreStarted")
        }

        override fun onRestoreCompleted(customerInfo: CustomerInfo) {
            Log.d(TAG, "onRestoreCompleted")
        }

        override fun onRestoreError(error: PurchasesError) {
            Log.d(TAG, "onRestoreError: ${error.message}")
        }
    }

    fun launchCustomerCenter() {
        customerCenter.launch(Unit)
    }

    fun launchPaywallFooterViewAsActivity(offering: Offering? = null) {
        // WIP: Change to use PaywallActivityLauncher
        val intent = Intent(this, PaywallFooterViewActivity::class.java)
        offering?.identifier?.let { intent.putExtra("offering_id", it) }
        startActivity(intent)
    }

    fun launchPaywallViewAsActivity(offering: Offering? = null) {
        // WIP: Change to use PaywallActivityLauncher
        val intent = Intent(this, PaywallViewActivity::class.java)
        offering?.identifier?.let { intent.putExtra("offering_id", it) }
        startActivity(intent)
    }
}
