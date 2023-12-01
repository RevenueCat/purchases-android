package com.revenuecat.paywallstester

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.paywallstester.databinding.ActivityPaywallViewBinding
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener

@OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
class PaywallViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val offeringId = intent.getStringExtra("offering_id")
        val binding = ActivityPaywallViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.paywallView.setOfferingId(offeringId)
        binding.paywallView.setPaywallListener(object : PaywallListener {
            override fun onPurchaseStarted(rcPackage: Package) {
                super.onPurchaseStarted(rcPackage)
                Log.d("PaywallsTester", "onPurchaseStarted")
            }

            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                super.onPurchaseCompleted(customerInfo, storeTransaction)
                Log.d("PaywallsTester", "onPurchaseCompleted")
            }
        })
        binding.paywallView.setDismissHandler { finish() }
    }
}
