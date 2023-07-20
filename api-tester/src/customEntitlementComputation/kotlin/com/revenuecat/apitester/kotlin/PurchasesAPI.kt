package com.revenuecat.apitester.kotlin

import android.content.Context
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.models.BillingFeature
import java.util.concurrent.ExecutorService

@Suppress("unused", "UNUSED_VARIABLE", "EmptyFunctionBlock")
private class PurchasesAPI {

    @Suppress("ForbiddenComment")
    fun checkConfiguration(context: Context, executorService: ExecutorService) {
        val features: List<BillingFeature> = ArrayList()
        val configured: Boolean = Purchases.isConfigured

        val build = PurchasesConfiguration.Builder(context, apiKey = "", appUserID = "")
            .observerMode(true)
            .observerMode(false)
            .service(executorService)
            .diagnosticsEnabled(true)
            .entitlementVerificationMode(EntitlementVerificationMode.INFORMATIONAL)
            .build()

        Purchases.configure(build)
    }
}
