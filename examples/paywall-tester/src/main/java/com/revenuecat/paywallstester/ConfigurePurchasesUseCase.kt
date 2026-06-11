package com.revenuecat.paywallstester

import android.content.Context
import com.revenuecat.purchases.DangerousSettings
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration

internal class ConfigurePurchasesUseCase(
    private val context: Context,
) {

    @OptIn(InternalRevenueCatAPI::class)
    operator fun invoke(apiKey: String, useWorkflows: Boolean = false) {
        val dangerousSettings = if (useWorkflows) {
            DangerousSettings.forWorkflows()
        } else {
            DangerousSettings()
        }

        val builder = PurchasesConfiguration.Builder(context.applicationContext, apiKey)
            .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
            .appUserID(null)
            .diagnosticsEnabled(true)
            .dangerousSettings(dangerousSettings)

        if (Constants.PREFERRED_UI_LOCALE_OVERRIDE.isNotEmpty()) {
            builder.preferredUILocaleOverride(
                Constants.PREFERRED_UI_LOCALE_OVERRIDE,
            )
        }

        Purchases.configure(builder.build())
    }
}
