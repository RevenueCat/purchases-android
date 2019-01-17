package com.revenuecat.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.interfaces.ReceivePurchaserInfoListener

class InitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        Purchases.sharedInstance.getPurchaserInfo(ReceivePurchaserInfoListener { purchaserInfo, error ->
            if (error != null) {
                Log.e("Purchases sample", "Error $error")
            } else if (purchaserInfo != null) {
                if (purchaserInfo.activeEntitlements.contains("pro")) {
                    startCats()
                } else {
                    startUpsell()
                }
            } else {
                Log.e("Purchases sample", "Unexpected error")
            }
        })
    }
}
