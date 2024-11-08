package com.revenuecat.webpurchaseredemptionsample

import android.app.Application
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration

class MainApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        Purchases.configure(PurchasesConfiguration.Builder(this, Constants.API_KEY).build())
    }
}
