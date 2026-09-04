package com.revenuecat.checkpointtester

import android.app.Application
import com.revenuecat.checkpointtester.checkpoints.CheckpointEventLog
import com.revenuecat.checkpointtester.checkpoints.DummyOfferingPaywallPresenter
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointListener
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.checkpointOfferingPresenter

class MainApplication : Application() {

    @OptIn(InternalRevenueCatAPI::class)
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.VERBOSE
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.API_KEY)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
        Purchases.sharedInstance.checkpointListener = CheckpointEventLog
        Purchases.sharedInstance.checkpointOfferingPresenter = DummyOfferingPaywallPresenter
    }
}
