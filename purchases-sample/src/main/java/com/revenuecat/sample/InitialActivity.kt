package com.revenuecat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith

class InitialActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_initial)
    }

    override fun onResume() {
        super.onResume()

        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { purchaserInfo ->
            if (purchaserInfo.entitlements[PREMIUM_ENTITLEMENT_ID]?.isActive == true) {
                startCatsActivity(true)
            } else {
                startUpsellActivity(true)
            }
            finish()
        }
    }
}
