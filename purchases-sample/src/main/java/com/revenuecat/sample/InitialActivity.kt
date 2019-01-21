package com.revenuecat.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.Purchases

class InitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)

        Purchases.sharedInstance.getPurchaserInfo({ purchaserInfo ->
            if (purchaserInfo.activeEntitlements.contains("pro")) {
                startCats()
            } else {
                startUpsell()
            }
        }, { error ->
            Log.e("Purchases sample", "Error $error")
        })
    }
}
