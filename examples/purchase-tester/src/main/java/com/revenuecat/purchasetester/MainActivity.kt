package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases_sample.R

class MainActivity : AppCompatActivity() {

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        if (Purchases.isConfigured) {
            Purchases.sharedInstance.handleDeepLink(intent)
        }
    }

    @OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (intent != null) {
            Purchases.sharedInstance.handleDeepLink(intent)
        }
    }
}
