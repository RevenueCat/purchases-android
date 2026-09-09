package com.revenuecat.checkpointssample

import android.app.Application
import com.revenuecat.checkpointssample.paywall.SamplePaywallPresenter
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.ui.revenuecatui.checkpoints.paywallPresenter

class MainApplication : Application() {
    @OptIn(InternalRevenueCatAPI::class)
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.VERBOSE
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.API_KEY)
                .appUserID(null)
                .build(),
        )
        // Offerings that checkpoints resolve to are presented by the sample's own paywall instead of the SDK's.
        Purchases.sharedInstance.paywallPresenter = SamplePaywallPresenter
    }
}
