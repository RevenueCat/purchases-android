package com.revenuecat.purchases

import android.content.Context
import com.revenuecat.purchases.common.BillingAbstract

internal fun Purchases.Companion.configureSdk(
    context: Context,
    appUserID: String,
    billingAbstract: BillingAbstract,
    entitlementVerificationMode: EntitlementVerificationMode? = null,
    forceServerErrors: Boolean = false,
    forceSigningErrors: Boolean = false,
) {
    Purchases.configure(
        PurchasesConfiguration.Builder(context, Constants.apiKey)
            .appUserID(appUserID)
            .apply {
                if (entitlementVerificationMode != null) {
                    entitlementVerificationMode(entitlementVerificationMode)
                }
            }
            .build(),
        billingAbstract,
        forceServerErrors,
        forceSigningErrors,
    )
}
