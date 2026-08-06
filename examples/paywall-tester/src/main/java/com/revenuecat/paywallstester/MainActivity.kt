@file:OptIn(ExperimentalComposeUiApi::class)

package com.revenuecat.paywallstester

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import com.revenuecat.paywallstester.ui.theme.PaywallTesterAndroidTheme
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.paywalls.PaywallAssetWarmer
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.Paywall
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.PaywallOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLaunchOptions
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.components.webview.PaywallWebViewPrewarming
import com.revenuecat.purchases.ui.revenuecatui.customercenter.ShowCustomerCenter
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable
import java.util.ServiceLoader

class MainActivity : ComponentActivity(), PaywallResultHandler {
    companion object {
        private const val TAG = "PaywallsTester"
        private const val DEFAULT_PREWARM_LEAD_MS = 3_000
    }

    private lateinit var paywallActivityLauncher: PaywallActivityLauncher
    private val customerCenter = registerForActivityResult(ShowCustomerCenter()) {}

    // Set by the web_view prewarming harness only; PaywallActivityLauncher refetches by offering id,
    // so the measured paywall is rendered in-process instead.
    private var prewarmOffering by mutableStateOf<Offering?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paywallActivityLauncher = PaywallActivityLauncher(this, this)
        handleWebViewPrewarmExtras()
        setContent {
            PaywallTesterAndroidTheme(dynamicColor = false) {
                Box(
                    modifier = Modifier
                        .semantics { testTagsAsResourceId = true }
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                ) {
                    PaywallTesterApp()
                    prewarmOffering?.let { offering ->
                        Paywall(
                            PaywallOptions.Builder(dismissRequest = { prewarmOffering = null })
                                .setOffering(offering)
                                .build(),
                        )
                    }
                }
            }
        }
    }

    /**
     * Dev-only: prewarms a `web_view` bundle, waits, then shows the paywall containing it, so the
     * prerender path can be timed end to end on a device.
     *
     * [prewarm_fit_width]/[prewarm_fit_height] must match the target component's Fit axes: a
     * prewarmed view is only adopted by a component with the identical configuration.
     *
     * adb shell am start -n <pkg>/.MainActivity \
     *   --es prewarm_url <https url> --es prewarm_component_id <id> \
     *   --es prewarm_offering <offering id> [--ei prewarm_lead_ms 3000] [--ez prewarm_startup true] \
     *   [--ez prewarm_fit_width true] [--ez prewarm_fit_height true]
     */
    @OptIn(InternalRevenueCatAPI::class)
    @Suppress("ReturnCount")
    private fun handleWebViewPrewarmExtras() {
        val url = intent.getStringExtra("prewarm_url") ?: return
        val componentId = intent.getStringExtra("prewarm_component_id") ?: return
        val offeringId = intent.getStringExtra("prewarm_offering") ?: return
        val leadMs = intent.getIntExtra("prewarm_lead_ms", DEFAULT_PREWARM_LEAD_MS).toLong()
        val sizeToContentWidth = intent.getBooleanExtra("prewarm_fit_width", false)
        val sizeToContentHeight = intent.getBooleanExtra("prewarm_fit_height", false)
        if (intent.getBooleanExtra("prewarm_startup", false)) {
            // Through ServiceLoader, the same way core reaches it, rather than the internal impl.
            ServiceLoader.load(PaywallAssetWarmer::class.java, PaywallAssetWarmer::class.java.classLoader)
                .firstOrNull()?.prebootWebView(this)
        }
        // Started before the offerings fetch, which prewarm needs none of, so leadMs measures actual
        // prewarm lead time instead of being inflated by an unrelated network round trip.
        val prewarmStartedAtMs = System.currentTimeMillis()
        PaywallWebViewPrewarming.prewarm(this, url, componentId, sizeToContentWidth, sizeToContentHeight)
        Purchases.sharedInstance.getOfferingsWith(
            onError = { error -> Log.e(TAG, "prewarm harness could not fetch offerings: $error") },
        ) { offerings ->
            val offering = offerings.all[offeringId] ?: run {
                Log.e(TAG, "prewarm harness could not find offering '$offeringId'")
                return@getOfferingsWith
            }
            if (isFinishing || isDestroyed) return@getOfferingsWith
            val remainingDelayMs = (leadMs - (System.currentTimeMillis() - prewarmStartedAtMs)).coerceAtLeast(0)
            Handler(Looper.getMainLooper()).postDelayed(
                {
                    if (!isFinishing && !isDestroyed) prewarmOffering = offering
                },
                remainingDelayMs,
            )
        }
    }

    override fun onActivityResult(result: PaywallResult) {
        // TODO-PAYWALLS: Handle result
        Log.e(TAG, "LAUNCH PAYWALL RESULT: $result")
    }

    @OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
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

        override fun onUrlOpened(url: String) {
            Log.d(TAG, "onUrlOpened: $url")
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
