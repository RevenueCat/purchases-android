package com.revenuecat.paywallstester

import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import com.revenuecat.paywallstester.databinding.ActivityPaywallViewBinding
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import com.revenuecat.purchases.ui.revenuecatui.utils.Resumable

class PaywallViewActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For extending content behind the Navigation Bar as well
        val window = window
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS)

        WindowCompat.setDecorFitsSystemWindows(window, false)

        val offeringId = intent.getStringExtra("offering_id")
        val binding = ActivityPaywallViewBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.paywallView.setOfferingId(offeringId)
        binding.paywallView.setPaywallListener(object : PaywallListener {
            override fun onPurchasePackageInitiated(rcPackage: Package, resume: Resumable) {
                AlertDialog
                    .Builder(this@PaywallViewActivity)
                    .setTitle("On Purchase Initiated Hook")
                    .setMessage(
                        "Example of presenting other UI over the paywall. Do you want to proceed with the purchase?",
                    )
                    .setPositiveButton("Proceed") { dialog, which -> resume() }
                    .setNegativeButton("Cancel") { dialog, which ->
                        dialog.dismiss()
                        resume(false)
                    }
                    .setCancelable(false)
                    .create()
                    .show()
            }

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
