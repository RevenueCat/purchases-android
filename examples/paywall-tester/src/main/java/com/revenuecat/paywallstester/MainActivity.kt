package com.revenuecat.paywallstester

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
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallActivityLauncher
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResult
import com.revenuecat.purchases.ui.revenuecatui.activity.PaywallResultHandler
import com.revenuecat.purchases.ui.revenuecatui.fonts.FontResourceProvider
import com.revenuecat.purchases.ui.revenuecatui.fonts.TypographyType

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
        paywallActivityLauncher.launch(
            offering,
            object : FontResourceProvider {
                override fun getFontResourceId(type: TypographyType): Int? {
                    return R.font.super_mario
                }
            },
        )
    }
}
