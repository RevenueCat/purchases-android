package com.revenuecat.paywallstester

import android.content.Context
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration

internal class ConfigurePurchasesUseCase(
    private val context: Context,
) {

    operator fun invoke(apiKey: String) {
        Purchases.configure(
            PurchasesConfiguration.Builder(context.applicationContext, apiKey)
                .purchasesAreCompletedBy(PurchasesAreCompletedBy.REVENUECAT)
                .appUserID(null)
                .diagnosticsEnabled(true)
                .build(),
        )
    }
}
