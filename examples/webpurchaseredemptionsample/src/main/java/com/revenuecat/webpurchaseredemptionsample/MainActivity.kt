package com.revenuecat.webpurchaseredemptionsample

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.webpurchaseredemptionsample.ui.theme.PurchasesandroidTheme

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainActivity : ComponentActivity() {

    internal var webPurchaseRedemption: WebPurchaseRedemption? by mutableStateOf(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        webPurchaseRedemption = Purchases.parseAsWebPurchaseRedemption(intent)
        enableEdgeToEdge()
        setContent {
            PurchasesandroidTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Content(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        webPurchaseRedemption = Purchases.parseAsWebPurchaseRedemption(intent)
    }

    fun clearWebPurchaseRedemption() {
        webPurchaseRedemption = null
    }
}
