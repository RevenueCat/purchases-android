package com.revenuecat.paywallstester

import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import java.net.URL

internal class ConfigurePurchasesUseCase(
    private val context: Context,
) {

    operator fun invoke(apiKey: String) {
        Purchases.proxyURL = URL("http://localhost:8000")

        val builder = PurchasesConfiguration.Builder(context.applicationContext, apiKey)
            .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
            .appUserID(null)
            .diagnosticsEnabled(true)

        if (Constants.PREFERRED_UI_LOCALE_OVERRIDE.isNotEmpty()) {
            builder.preferredUILocaleOverride(
                Constants.PREFERRED_UI_LOCALE_OVERRIDE,
            )
        }

        Purchases.configure(builder.build())
    }
}
