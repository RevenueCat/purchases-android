package com.revenuecat.apitester.kotlin

import android.content.Context
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.galaxy.GalaxyBillingMode
import com.revenuecat.purchases.galaxy.GalaxyConfiguration

@Suppress("unused", "UNUSED_VARIABLE")
private class GalaxyConfigurationAPI {
    fun checkConstructors(
        context: Context,
        apiKey: String,
        galaxyBillingMode: GalaxyBillingMode,
    ) {
        val builder1: GalaxyConfiguration.Builder = GalaxyConfiguration.Builder(
            context,
            apiKey,
        )

        val builder2: GalaxyConfiguration.Builder = GalaxyConfiguration.Builder(
            context,
            apiKey,
            galaxyBillingMode,
        )

        val config1: PurchasesConfiguration = builder1.build()
        val config2: PurchasesConfiguration = builder2.build()
    }

    fun checkGalaxyBillingModeFunction(
        context: Context,
        apiKey: String,
        galaxyBillingMode: GalaxyBillingMode,
    ) {
        val config: PurchasesConfiguration = GalaxyConfiguration.Builder(
            context,
            apiKey,
        )
            .galaxyBillingMode(galaxyBillingMode = galaxyBillingMode)
            .build()
    }
}
