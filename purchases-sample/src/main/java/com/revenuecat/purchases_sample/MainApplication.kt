package com.revenuecat.purchases_sample

import android.app.Application
import com.revenuecat.purchases.Purchases

private val PURCHASES_KEY = "LQmxAoIaaQaHpPiWJJayypBDhIpAZCZN"

class MainApplication: Application() {

    override fun onCreate() {
        super.onCreate()
        Purchases.sharedInstance = Purchases.Builder(this, PURCHASES_KEY).build()
        val instance = Purchases.sharedInstance
    }

}