package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.asWebPurchaseRedemption
import com.revenuecat.purchasetester.ui.PurchaseTesterApp
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterTheme

class MainActivity : ComponentActivity() {

    internal var webPurchaseRedemption by mutableStateOf<WebPurchaseRedemption?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        webPurchaseRedemption = intent.asWebPurchaseRedemption()

        setContent {
            PurchaseTesterTheme {
                PurchaseTesterApp(
                    webPurchaseRedemption = webPurchaseRedemption,
                    onWebPurchaseRedemptionConsume = { clearWebPurchaseRedemption() },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        webPurchaseRedemption = intent.asWebPurchaseRedemption()
    }

    fun clearWebPurchaseRedemption() {
        webPurchaseRedemption = null
    }
}
