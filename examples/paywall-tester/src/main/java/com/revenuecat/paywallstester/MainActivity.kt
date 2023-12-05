package com.revenuecat.paywallstester

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.revenuecat.paywallstester.ui.theme.PaywallTesterAndroidTheme
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
class MainActivity : ComponentActivity(), PaywallResultHandler {
    private lateinit var paywallActivityLauncher: PaywallActivityLauncher

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        paywallActivityLauncher = PaywallActivityLauncher(this, this)
        setContent {
            PaywallTesterAndroidTheme {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PaywallTesterApp()
                }
            }
        }
    }

    override fun onActivityResult(result: PaywallResult) {
        // TODO-PAYWALLS: Handle result
        Log.e("PaywallsTester", "LAUNCH PAYWALL RESULT: $result")
    }

    fun launchPaywall(offering: Offering? = null) {
        paywallActivityLauncher.launch(offering)
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
