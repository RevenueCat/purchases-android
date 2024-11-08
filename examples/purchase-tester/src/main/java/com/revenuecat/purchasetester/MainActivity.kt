package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases_sample.R

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class MainActivity : AppCompatActivity() {

    internal var webPurchaseRedemption: WebPurchaseRedemption? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        webPurchaseRedemption = Purchases.parseAsWebPurchaseRedemption(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent != null) {
            webPurchaseRedemption = Purchases.parseAsWebPurchaseRedemption(intent)
        }
    }

    fun clearWebPurchaseRedemption() {
        webPurchaseRedemption = null
    }
}
