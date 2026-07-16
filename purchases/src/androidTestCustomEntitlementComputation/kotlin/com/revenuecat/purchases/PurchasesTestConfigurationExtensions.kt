package com.revenuecat.purchases

import android.content.Context
import com.revenuecat.purchases.common.BillingAbstract

@Suppress("LongParameterList")
internal fun Purchases.Companion.configureSdk(
    context: Context,
    appUserID: String,
    billingAbstract: BillingAbstract,
    entitlementVerificationMode: EntitlementVerificationMode? = null,
    forceServerErrorStrategy: ForceServerErrorStrategy? = null,
    forceSigningErrors: Boolean = false,
    dangerousSettings: DangerousSettings? = null,
) {
    Purchases.configure(
        PurchasesConfiguration.Builder(context, Constants.apiKey)
            .appUserID(appUserID)
            .dangerousSettings(dangerousSettings ?: DangerousSettings(customEntitlementComputation = true))
            .apply {
                if (entitlementVerificationMode != null) {
                    entitlementVerificationMode(entitlementVerificationMode)
                }
            }
            .build(),
        billingAbstract,
        forceServerErrorStrategy,
        forceSigningErrors,
    )
}
