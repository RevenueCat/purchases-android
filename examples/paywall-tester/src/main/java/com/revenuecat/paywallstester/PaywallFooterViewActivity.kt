package com.revenuecat.paywallstester

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.paywallstester.databinding.ActivityPaywallFooterViewBinding
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener

class PaywallFooterViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val offeringId = intent.getStringExtra("offering_id")
        val binding = ActivityPaywallFooterViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.paywallFooterView.setOfferingId(offeringId)
        binding.paywallFooterView.setPaywallListener(object : PaywallListener {
            override fun onPurchaseStarted(rcPackage: Package) {
                super.onPurchaseStarted(rcPackage)
                Log.d("PaywallsTester", "onPurchaseStarted")
            }

            override fun onPurchaseCompleted(customerInfo: CustomerInfo, storeTransaction: StoreTransaction) {
                super.onPurchaseCompleted(customerInfo, storeTransaction)
                Log.d("PaywallsTester", "onPurchaseCompleted")
            }
        })
        binding.paywallFooterView.setDismissHandler { finish() }
    }
}
