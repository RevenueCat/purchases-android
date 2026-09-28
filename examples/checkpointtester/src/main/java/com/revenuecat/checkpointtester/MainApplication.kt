package com.revenuecat.checkpointtester

import android.app.Application
import com.revenuecat.checkpointtester.checkpoints.PaywallPresenters
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI

class MainApplication : Application() {

    @OptIn(InviteOnlyCheckpointsAPI::class)
    override fun onCreate() {
        super.onCreate()
        Purchases.logLevel = LogLevel.VERBOSE
        Purchases.configure(
            PurchasesConfiguration.Builder(this, Constants.API_KEY)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
        PaywallPresenters.select(PaywallPresenters.mode.value)
    }
}
