package com.revenuecat.sample.ui.paywall

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.revenuecat.sample.R

class PaywallActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        /*
        Paywall fragment setup in activity_paywall.xml
         */
        setContentView(R.layout.activity_paywall)
    }
}
